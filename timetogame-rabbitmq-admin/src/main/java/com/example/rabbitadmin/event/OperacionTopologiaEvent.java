package com.example.rabbitadmin.event;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cambio realizado en la topologia del broker. RabbitAdminService lo publica
 * como evento de aplicacion de Spring y la capa de mensajeria lo reenvia a
 * RabbitMQ; tambien es el payload JSON del mensaje de auditoria.
 */
public record OperacionTopologiaEvent(
        @NotBlank @Size(max = 36) String eventId,
        @NotNull TipoOperacion operacion,
        @NotNull TipoRecurso recurso,
        @NotBlank @Size(max = 600) String descripcion,
        @Size(max = 120) String usuario,
        @NotNull Instant ocurridoEn
) {

    public static OperacionTopologiaEvent de(TipoOperacion operacion, TipoRecurso recurso, String descripcion,
                                             String usuario) {
        return new OperacionTopologiaEvent(UUID.randomUUID().toString(), operacion, recurso, descripcion, usuario,
                Instant.now());
    }
}
