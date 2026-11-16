package com.example.Timetogame.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.example.Timetogame.dto.CrearResenaRequest;
import com.example.Timetogame.dto.ResenaDTO;
import com.example.Timetogame.dto.VideojuegoDTO;
import com.example.Timetogame.entity.Resena;
import com.example.Timetogame.exception.ResourceNotFoundException;
import com.example.Timetogame.repository.ResenaRepository;
import com.example.Timetogame.security.UsuarioActualProvider;
import com.example.Timetogame.service.VideojuegoService;
import com.example.Timetogame.streaming.event.ActividadEvent;
import com.example.Timetogame.streaming.event.TipoActividad;

class ResenaServiceImplTest {

    private final ResenaRepository repository = mock(ResenaRepository.class);
    private final VideojuegoService videojuegoService = mock(VideojuegoService.class);
    private final UsuarioActualProvider usuarioActualProvider = mock(UsuarioActualProvider.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ResenaServiceImpl service = new ResenaServiceImpl(repository, videojuegoService,
            usuarioActualProvider, eventPublisher);

    private final VideojuegoDTO halo = new VideojuegoDTO(7L, "Halo", null, BigDecimal.TEN, null, 1L, "FPS");

    @Test
    void guardaLaResenaYLaPublicaComoActividad() {
        when(videojuegoService.findById(7L)).thenReturn(halo);
        when(usuarioActualProvider.usuarioActual()).thenReturn("ana@test");
        when(repository.save(any(Resena.class))).thenAnswer(inv -> inv.getArgument(0));

        ResenaDTO creada = service.crear(7L, new CrearResenaRequest(4, "  Muy bueno  "));

        assertThat(creada.usuario()).isEqualTo("ana@test");
        assertThat(creada.comentario()).isEqualTo("Muy bueno");
        ArgumentCaptor<ActividadEvent> evento = ArgumentCaptor.forClass(ActividadEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(TipoActividad.RESENA);
        assertThat(evento.getValue().calificacion()).isEqualTo(4);
        assertThat(evento.getValue().isDatosCoherentesConElTipo()).isTrue();
    }

    @Test
    void noPublicaSiElVideojuegoNoExiste() {
        when(videojuegoService.findById(99L)).thenThrow(new ResourceNotFoundException("no existe"));

        assertThatThrownBy(() -> service.crear(99L, new CrearResenaRequest(5, "x")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void unaVistaNoPuedeTraerDatosDeResena() {
        ActividadEvent vista = ActividadEvent.vista(7L, "Halo", "ana", null);
        ActividadEvent mal = new ActividadEvent(vista.eventId(), vista.ocurridoEn(), TipoActividad.VISTA, 7L, "Halo",
                "ana", vista.origen(), 5, "no corresponde");

        assertThat(vista.isDatosCoherentesConElTipo()).isTrue();
        assertThat(mal.isDatosCoherentesConElTipo()).isFalse();
    }
}
