package com.example.Timetogame.service.impl;

import org.springframework.stereotype.Service;

import com.example.Timetogame.dto.AvisoCatalogoDTO;
import com.example.Timetogame.dto.ResenaDTO;
import com.example.Timetogame.service.AvisosEnVivoService;
import com.example.Timetogame.streaming.event.ActividadEvent;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;
import com.example.Timetogame.streaming.sse.StreamHub;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvisosEnVivoServiceImpl implements AvisosEnVivoService {

    private final StreamHub streamHub;

    @Override
    public void notificarCambio(CatalogoCambioEvent evento) {
        String accion = switch (evento.tipo()) {
            case CREADO -> "se agrego al catalogo";
            case ACTUALIZADO -> "se actualizo";
            case ELIMINADO -> "se retiro del catalogo";
        };
        streamHub.enviar("catalogo", new AvisoCatalogoDTO(evento.eventId(), evento.tipo(), evento.videojuegoId(),
                evento.titulo(), "\"%s\" %s".formatted(evento.titulo(), accion), evento.ocurridoEn()));
    }

    /** Evento SSE "resena": la portada la agrega arriba de "Resenas recientes". */
    @Override
    public void notificarResena(ActividadEvent resena) {
        streamHub.enviar("resena", new ResenaDTO(null, resena.videojuegoId(), resena.titulo(), resena.usuario(),
                resena.calificacion(), resena.comentario(), resena.ocurridoEn()));
    }
}
