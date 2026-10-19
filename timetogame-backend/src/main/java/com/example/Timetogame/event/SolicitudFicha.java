package com.example.Timetogame.event;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Pedido de generar (o eliminar) la ficha tecnica de un videojuego. Es a la
 * vez evento de aplicacion (lo publica el servicio) y cuerpo del mensaje que
 * viaja por RabbitMQ.
 */
public record SolicitudFicha(
        @NotBlank @Size(max = 36) String solicitudId,
        @NotNull AccionFicha accion,
        @NotNull @Positive Long videojuegoId,
        @Size(max = 120) String solicitadoPor,
        @NotNull Instant solicitadoEn
) {

    public static SolicitudFicha de(AccionFicha accion, Long videojuegoId, String solicitadoPor) {
        return new SolicitudFicha(UUID.randomUUID().toString(), accion, videojuegoId, solicitadoPor, Instant.now());
    }
}
