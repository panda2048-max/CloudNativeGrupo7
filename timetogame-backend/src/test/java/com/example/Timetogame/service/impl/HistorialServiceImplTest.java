package com.example.Timetogame.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.Timetogame.repository.HistorialVistaRepository;
import com.example.Timetogame.security.UsuarioActualProvider;
import com.example.Timetogame.streaming.event.OrigenActividad;
import com.example.Timetogame.streaming.event.ActividadEvent;
import com.example.Timetogame.streaming.event.TipoActividad;

class HistorialServiceImplTest {

    private final HistorialVistaRepository repository = mock(HistorialVistaRepository.class);
    private final HistorialServiceImpl service = new HistorialServiceImpl(repository, mock(UsuarioActualProvider.class));

    @Test
    void guardaLasVistasDeUsuariosConSesion() {
        assertThat(service.registrar(vista("ana", OrigenActividad.DETALLE))).isTrue();
        verify(repository).save(any());
    }

    @Test
    void ignoraVisitantesYSimulacion() {
        assertThat(service.registrar(vista(null, OrigenActividad.DETALLE))).isFalse();
        assertThat(service.registrar(vista("simulador", OrigenActividad.SIMULACION))).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void esIdempotente() {
        ActividadEvent vista = vista("ana", OrigenActividad.DETALLE);
        when(repository.existsByEventId(vista.eventId())).thenReturn(true);

        assertThat(service.registrar(vista)).isFalse();
        verify(repository, never()).save(any());
    }

    private static ActividadEvent vista(String usuario, OrigenActividad origen) {
        return new ActividadEvent("e-" + System.nanoTime(), Instant.now(), TipoActividad.VISTA, 7L, "Halo", usuario,
                origen, null, null);
    }
}
