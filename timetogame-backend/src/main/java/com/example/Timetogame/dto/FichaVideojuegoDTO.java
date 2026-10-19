package com.example.Timetogame.dto;

import java.time.Instant;

public record FichaVideojuegoDTO(
        Long videojuegoId,
        String titulo,
        String contenido,
        int version,
        String solicitadoPor,
        Instant generadoEn
) {
}
