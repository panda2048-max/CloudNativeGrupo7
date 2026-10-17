package com.example.Timetogame.dto;

import java.time.Instant;

/** Lectura de un mensaje fallido; tambien sirve para registrarlo (id y registradoEn nulos). */
public record MensajeFallidoDTO(
        Long id,
        String messageId,
        String colaOrigen,
        String routingKey,
        String motivo,
        int reintentos,
        String ultimoError,
        String payload,
        Instant registradoEn
) {
}
