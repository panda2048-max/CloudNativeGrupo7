package com.example.Timetogame.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Timetogame.dto.NotificacionDTO;
import com.example.Timetogame.entity.Notificacion;
import com.example.Timetogame.event.CatalogoEvent;
import com.example.Timetogame.repository.NotificacionRepository;
import com.example.Timetogame.service.NotificacionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Override
    public boolean registrar(CatalogoEvent evento) {
        if (notificacionRepository.existsByEventId(evento.eventId())) {
            return false;
        }

        notificacionRepository.save(Notificacion.builder()
                .eventId(evento.eventId())
                .tipoEvento(evento.tipo())
                .entidad(evento.entidad())
                .entidadId(evento.entidadId())
                .mensaje(construirMensaje(evento))
                .usuario(evento.usuario())
                .ocurridoEn(evento.ocurridoEn())
                .procesadoEn(Instant.now())
                .build());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionDTO> findRecientes() {
        return notificacionRepository.findTop50ByOrderByProcesadoEnDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    private String construirMensaje(CatalogoEvent evento) {
        String accion = switch (evento.tipo()) {
            case CREADO -> "creo";
            case ACTUALIZADO -> "actualizo";
            case ELIMINADO -> "elimino";
        };
        String entidad = switch (evento.entidad()) {
            case VIDEOJUEGO -> "el videojuego";
            case GENERO -> "el genero";
        };
        String autor = evento.usuario() != null ? evento.usuario() : "Un administrador";
        return "%s %s %s '%s' (id %d)".formatted(autor, accion, entidad, evento.nombre(), evento.entidadId());
    }

    private NotificacionDTO toDTO(Notificacion notificacion) {
        return new NotificacionDTO(
                notificacion.getId(),
                notificacion.getTipoEvento(),
                notificacion.getEntidad(),
                notificacion.getEntidadId(),
                notificacion.getMensaje(),
                notificacion.getUsuario(),
                notificacion.getOcurridoEn(),
                notificacion.getProcesadoEn()
        );
    }
}
