package com.example.Timetogame.dto;

import java.time.Instant;

/** Respuesta 202 Accepted: la ficha se genera despues, en el consumidor. */
public record SolicitudFichaDTO(
        String solicitudId,
        Long videojuegoId,
        String estado,
        Instant solicitadoEn
) {
}
