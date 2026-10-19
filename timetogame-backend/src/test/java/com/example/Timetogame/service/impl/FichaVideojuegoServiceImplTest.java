package com.example.Timetogame.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.example.Timetogame.dto.SolicitudFichaDTO;
import com.example.Timetogame.entity.FichaVideojuego;
import com.example.Timetogame.entity.Genero;
import com.example.Timetogame.entity.Videojuego;
import com.example.Timetogame.event.AccionFicha;
import com.example.Timetogame.event.SolicitudFicha;
import com.example.Timetogame.exception.ResourceNotFoundException;
import com.example.Timetogame.repository.FichaVideojuegoRepository;
import com.example.Timetogame.repository.VideojuegoRepository;
import com.example.Timetogame.security.UsuarioActualProvider;

class FichaVideojuegoServiceImplTest {

    private final FichaVideojuegoRepository fichaRepository = mock(FichaVideojuegoRepository.class);
    private final VideojuegoRepository videojuegoRepository = mock(VideojuegoRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UsuarioActualProvider usuarioActualProvider = mock(UsuarioActualProvider.class);
    private final FichaVideojuegoServiceImpl service = new FichaVideojuegoServiceImpl(fichaRepository,
            videojuegoRepository, eventPublisher, usuarioActualProvider);

    private final Videojuego halo = Videojuego.builder()
            .id(7L).titulo("Halo").descripcion("Shooter").precio(new BigDecimal("19990"))
            .fechaLanzamiento(LocalDate.of(2001, 11, 15)).genero(Genero.builder().id(1L).nombre("FPS").build())
            .build();

    @Test
    void generaLaPrimeraVersionDeLaFicha() {
        when(videojuegoRepository.findById(7L)).thenReturn(Optional.of(halo));
        when(fichaRepository.findByVideojuegoId(7L)).thenReturn(Optional.empty());

        boolean generada = service.generar(SolicitudFicha.de(AccionFicha.GENERAR, 7L, "admin@test"));

        ArgumentCaptor<FichaVideojuego> ficha = ArgumentCaptor.forClass(FichaVideojuego.class);
        verify(fichaRepository).save(ficha.capture());
        assertThat(generada).isTrue();
        assertThat(ficha.getValue().getVersion()).isEqualTo(1);
        assertThat(ficha.getValue().getContenido()).contains("FICHA TECNICA - Halo", "FPS", "USD 19990.00", "15-11-2001");
    }

    @Test
    void noRegeneraUnaSolicitudYaAplicada() {
        SolicitudFicha solicitud = SolicitudFicha.de(AccionFicha.GENERAR, 7L, "admin@test");
        when(videojuegoRepository.findById(7L)).thenReturn(Optional.of(halo));
        when(fichaRepository.findByVideojuegoId(7L)).thenReturn(Optional.of(FichaVideojuego.builder()
                .videojuegoId(7L).version(2).ultimaSolicitudId(solicitud.solicitudId()).build()));

        assertThat(service.generar(solicitud)).isFalse();
        verify(fichaRepository, never()).save(any());
    }

    @Test
    void rechazaGenerarLaFichaDeUnVideojuegoInexistente() {
        when(videojuegoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(SolicitudFicha.de(AccionFicha.GENERAR, 99L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void solicitarPublicaElEventoYResponde202ConLaSolicitud() {
        when(videojuegoRepository.existsById(7L)).thenReturn(true);
        when(usuarioActualProvider.usuarioActual()).thenReturn("admin@test");

        SolicitudFichaDTO respuesta = service.solicitarGeneracion(7L);

        ArgumentCaptor<SolicitudFicha> evento = ArgumentCaptor.forClass(SolicitudFicha.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().accion()).isEqualTo(AccionFicha.GENERAR);
        assertThat(evento.getValue().solicitadoPor()).isEqualTo("admin@test");
        assertThat(respuesta.solicitudId()).isEqualTo(evento.getValue().solicitudId());
        assertThat(respuesta.estado()).isEqualTo("EN_COLA");
    }
}
