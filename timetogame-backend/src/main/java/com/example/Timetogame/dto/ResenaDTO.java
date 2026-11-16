package com.example.Timetogame.dto;

import java.time.Instant;

public record ResenaDTO(
        Long id,
        Long videojuegoId,
        String tituloVideojuego,
        String usuario,
        int calificacion,
        String comentario,
        Instant creadaEn
) {
}
