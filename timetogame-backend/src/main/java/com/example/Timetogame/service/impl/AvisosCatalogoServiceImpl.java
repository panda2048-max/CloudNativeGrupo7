package com.example.Timetogame.service.impl;

import org.springframework.stereotype.Service;

import com.example.Timetogame.dto.AvisoCatalogoDTO;
import com.example.Timetogame.service.AvisosCatalogoService;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;
import com.example.Timetogame.streaming.sse.StreamHub;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvisosCatalogoServiceImpl implements AvisosCatalogoService {

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
}
