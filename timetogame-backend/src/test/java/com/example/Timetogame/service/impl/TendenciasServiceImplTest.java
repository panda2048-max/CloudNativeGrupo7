package com.example.Timetogame.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.Timetogame.dto.TendenciaDTO;
import com.example.Timetogame.event.TipoEventoCatalogo;
import com.example.Timetogame.streaming.config.StreamingProperties;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;
import com.example.Timetogame.streaming.event.OrigenActividad;
import com.example.Timetogame.streaming.event.ActividadEvent;
import com.example.Timetogame.streaming.event.TipoActividad;

class TendenciasServiceImplTest {

    private final TendenciasServiceImpl service = new TendenciasServiceImpl(new StreamingProperties(
            new StreamingProperties.Topicos(3, (short) 3, 2, Duration.ofDays(7), Duration.ofDays(14)),
            new StreamingProperties.Tendencias(Duration.ofMinutes(10), 2, 3, Duration.ZERO)));

    @Test
    void ordenaPorVistasYRespetaElTop() {
        vistas(1L, "Halo", 3);
        vistas(2L, "Doom", 5);
        vistas(3L, "Tetris", 1);

        assertThat(service.ranking()).extracting(TendenciaDTO::titulo).containsExactly("Doom", "Halo");
        assertThat(service.ranking().getFirst().vistas()).isEqualTo(5);
    }

    @Test
    void noCuentaDosVecesLaMismaEntrega() {
        ActividadEvent vista = vista(1L, "Halo", Instant.now());

        assertThat(service.registrarVista(vista)).isTrue();
        assertThat(service.registrarVista(vista)).isFalse();
        assertThat(service.ranking().getFirst().vistas()).isEqualTo(1);
    }

    @Test
    void ignoraVistasFueraDeLaVentana() {
        assertThat(service.registrarVista(vista(1L, "Halo", Instant.now().minus(Duration.ofMinutes(30))))).isFalse();
        assertThat(service.ranking()).isEmpty();
    }

    @Test
    void sacaDelRankingAlVideojuegoEliminadoYActualizaTitulos() {
        vistas(1L, "Halo", 2);
        vistas(2L, "Doom", 1);

        service.aplicarCambio(cambio(2L, "Doom Eternal", TipoEventoCatalogo.ACTUALIZADO));
        service.aplicarCambio(cambio(1L, "Halo", TipoEventoCatalogo.ELIMINADO));

        assertThat(service.ranking()).extracting(TendenciaDTO::titulo).containsExactly("Doom Eternal");
        assertThat(service.registrarVista(vista(1L, "Halo", Instant.now()))).isFalse();
    }

    @Test
    void informaCambiosUnaSolaVez() {
        service.consumirCambios();
        assertThat(service.consumirCambios()).isFalse();

        vistas(1L, "Halo", 1);
        assertThat(service.consumirCambios()).isTrue();
        assertThat(service.consumirCambios()).isFalse();
    }

    private void vistas(Long id, String titulo, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            service.registrarVista(vista(id, titulo, Instant.now()));
        }
    }

    private static ActividadEvent vista(Long id, String titulo, Instant cuando) {
        return new ActividadEvent(UUID.randomUUID().toString(), cuando, TipoActividad.VISTA, id, titulo, "ana",
                OrigenActividad.DETALLE, null, null);
    }

    private static CatalogoCambioEvent cambio(Long id, String titulo, TipoEventoCatalogo tipo) {
        return new CatalogoCambioEvent(UUID.randomUUID().toString(), Instant.now(), tipo, id, titulo, "admin");
    }
}
