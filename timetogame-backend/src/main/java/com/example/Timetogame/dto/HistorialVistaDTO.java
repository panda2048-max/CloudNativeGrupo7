package com.example.Timetogame.dto;

import java.time.Instant;

public record HistorialVistaDTO(
        Long videojuegoId,
        String titulo,
        Instant vistoEn
) {
}
