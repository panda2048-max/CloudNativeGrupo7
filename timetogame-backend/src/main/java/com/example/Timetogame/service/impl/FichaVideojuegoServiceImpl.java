package com.example.Timetogame.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Timetogame.dto.FichaVideojuegoDTO;
import com.example.Timetogame.dto.SolicitudFichaDTO;
import com.example.Timetogame.entity.FichaVideojuego;
import com.example.Timetogame.entity.Videojuego;
import com.example.Timetogame.event.AccionFicha;
import com.example.Timetogame.event.SolicitudFicha;
import com.example.Timetogame.exception.ResourceNotFoundException;
import com.example.Timetogame.repository.FichaVideojuegoRepository;
import com.example.Timetogame.repository.VideojuegoRepository;
import com.example.Timetogame.security.UsuarioActualProvider;
import com.example.Timetogame.service.FichaVideojuegoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FichaVideojuegoServiceImpl implements FichaVideojuegoService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Santiago"));

    private final FichaVideojuegoRepository fichaRepository;
    private final VideojuegoRepository videojuegoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    public SolicitudFichaDTO solicitarGeneracion(Long videojuegoId) {
        if (!videojuegoRepository.existsById(videojuegoId)) {
            throw new ResourceNotFoundException("Videojuego no encontrado con id: " + videojuegoId);
        }
        SolicitudFicha solicitud = SolicitudFicha.de(AccionFicha.GENERAR, videojuegoId,
                usuarioActualProvider.usuarioActual());
        eventPublisher.publishEvent(solicitud);
        return new SolicitudFichaDTO(solicitud.solicitudId(), videojuegoId, "EN_COLA", solicitud.solicitadoEn());
    }

    @Override
    public boolean generar(SolicitudFicha solicitud) {
        Videojuego videojuego = videojuegoRepository.findById(solicitud.videojuegoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se puede generar la ficha: el videojuego " + solicitud.videojuegoId() + " no existe"));

        FichaVideojuego ficha = fichaRepository.findByVideojuegoId(videojuego.getId())
                .orElseGet(() -> FichaVideojuego.builder().videojuegoId(videojuego.getId()).version(0).build());
        if (solicitud.solicitudId().equals(ficha.getUltimaSolicitudId())) {
            return false;
        }

        Instant ahora = Instant.now();
        ficha.setTitulo(videojuego.getTitulo());
        ficha.setVersion(ficha.getVersion() + 1);
        ficha.setUltimaSolicitudId(solicitud.solicitudId());
        ficha.setSolicitadoPor(solicitud.solicitadoPor());
        ficha.setGeneradoEn(ahora);
        ficha.setContenido(redactar(videojuego, ficha.getVersion(), solicitud, ahora));
        fichaRepository.save(ficha);
        return true;
    }

    @Override
    public void eliminar(SolicitudFicha solicitud) {
        fichaRepository.deleteByVideojuegoId(solicitud.videojuegoId());
    }

    @Override
    @Transactional(readOnly = true)
    public FichaVideojuegoDTO findByVideojuegoId(Long videojuegoId) {
        return fichaRepository.findByVideojuegoId(videojuegoId)
                .map(f -> new FichaVideojuegoDTO(f.getVideojuegoId(), f.getTitulo(), f.getContenido(),
                        f.getVersion(), f.getSolicitadoPor(), f.getGeneradoEn()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La ficha del videojuego " + videojuegoId + " aun no se ha generado"));
    }

    private static String redactar(Videojuego videojuego, int version, SolicitudFicha solicitud, Instant ahora) {
        String lanzamiento = videojuego.getFechaLanzamiento() != null
                ? videojuego.getFechaLanzamiento().format(FECHA) : "Sin fecha informada";
        String descripcion = videojuego.getDescripcion() != null && !videojuego.getDescripcion().isBlank()
                ? videojuego.getDescripcion() : "Sin descripcion.";
        String autor = solicitud.solicitadoPor() != null ? solicitud.solicitadoPor() : "sistema";

        return """
                FICHA TECNICA - %s
                ==================================================
                Genero:              %s
                Precio:              %s
                Fecha de lanzamiento: %s

                Descripcion
                %s

                --------------------------------------------------
                Version %d | generada %s | solicitada por %s
                """.formatted(videojuego.getTitulo(), videojuego.getGenero().getNombre(),
                formatearPrecio(videojuego.getPrecio()), lanzamiento, descripcion, version,
                FECHA_HORA.format(ahora), autor);
    }

    private static String formatearPrecio(BigDecimal precio) {
        return "USD " + precio.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
