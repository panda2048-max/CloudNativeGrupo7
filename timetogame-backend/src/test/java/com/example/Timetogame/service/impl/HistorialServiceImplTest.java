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
import com.example.Timetogame.streaming.event.OrigenVista;
import com.example.Timetogame.streaming.event.VideojuegoVistoEvent;

class HistorialServiceImplTest {

    private final HistorialVistaRepository repository = mock(HistorialVistaRepository.class);
    private final HistorialServiceImpl service = new HistorialServiceImpl(repository, mock(UsuarioActualProvider.class));

    @Test
    void guardaLasVistasDeUsuariosConSesion() {
        assertThat(service.registrar(vista("ana", OrigenVista.DETALLE))).isTrue();
        verify(repository).save(any());
    }

    @Test
    void ignoraVisitantesYSimulacion() {
        assertThat(service.registrar(vista(null, OrigenVista.DETALLE))).isFalse();
        assertThat(service.registrar(vista("simulador", OrigenVista.SIMULACION))).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void esIdempotente() {
        VideojuegoVistoEvent vista = vista("ana", OrigenVista.DETALLE);
        when(repository.existsByEventId(vista.eventId())).thenReturn(true);

        assertThat(service.registrar(vista)).isFalse();
        verify(repository, never()).save(any());
    }

    private static VideojuegoVistoEvent vista(String usuario, OrigenVista origen) {
        return new VideojuegoVistoEvent("e-" + System.nanoTime(), Instant.now(), 7L, "Halo", usuario, origen);
    }
}
