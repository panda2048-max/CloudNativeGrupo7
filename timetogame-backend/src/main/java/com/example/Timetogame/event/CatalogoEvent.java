package com.example.Timetogame.event;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Cambio ocurrido en el catalogo. Los servicios de negocio lo publican como
 * evento de aplicacion de Spring; la capa de mensajeria lo reenvia a RabbitMQ
 * recien cuando la transaccion confirma. Es tambien el payload JSON del
 * mensaje, por eso las validaciones: el consumidor no confia en lo que llega.
 */
public record CatalogoEvent(
        @NotBlank @Size(max = 36) String eventId,
        @NotNull TipoEventoCatalogo tipo,
        @NotNull EntidadCatalogo entidad,
        @NotNull @Positive Long entidadId,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 120) String usuario,
        @NotNull Instant ocurridoEn
) {

    public static CatalogoEvent de(TipoEventoCatalogo tipo, EntidadCatalogo entidad, Long entidadId,
                                   String nombre, String usuario) {
        return new CatalogoEvent(UUID.randomUUID().toString(), tipo, entidad, entidadId, nombre, usuario,
                Instant.now());
    }
}
