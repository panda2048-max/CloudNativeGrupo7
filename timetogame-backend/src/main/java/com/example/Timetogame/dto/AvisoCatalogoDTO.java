package com.example.Timetogame.dto;

import java.time.Instant;

import com.example.Timetogame.event.TipoEventoCatalogo;

/** Evento SSE "catalogo": un videojuego se creo, actualizo o elimino. */
public record AvisoCatalogoDTO(
        String eventId,
        TipoEventoCatalogo tipo,
        Long videojuegoId,
        String titulo,
        String mensaje,
        Instant ocurridoEn
) {
}
