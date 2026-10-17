package com.example.rabbitadmin.dto;

import static com.example.rabbitadmin.dto.ReglasNombre.MENSAJE_NOMBRE;
import static com.example.rabbitadmin.dto.ReglasNombre.MENSAJE_ROUTING_KEY;
import static com.example.rabbitadmin.dto.ReglasNombre.PATRON_NOMBRE;
import static com.example.rabbitadmin.dto.ReglasNombre.PATRON_ROUTING_KEY;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * No se expone "exclusive": una cola exclusiva pertenece a la conexion que la
 * declara (la de este microservicio) y desapareceria al cerrarse; no tiene
 * sentido crearla desde una API de administracion.
 */
public record CrearColaRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = PATRON_NOMBRE, message = "Nombre de cola invalido: " + MENSAJE_NOMBRE)
        String nombre,

        /** Por defecto true. */
        Boolean durable,

        /** Por defecto false. */
        Boolean autoDelete,

        /** Por defecto CLASSIC. */
        TipoCola tipo,

        @Pattern(regexp = PATRON_NOMBRE, message = "Dead letter exchange invalido: " + MENSAJE_NOMBRE)
        String deadLetterExchange,

        @Pattern(regexp = PATRON_ROUTING_KEY, message = "Dead letter routing key invalida: " + MENSAJE_ROUTING_KEY)
        String deadLetterRoutingKey,

        @Positive(message = "El TTL de los mensajes debe ser mayor que 0 ms")
        @Max(value = 604_800_000, message = "El TTL de los mensajes no puede superar 7 dias (604800000 ms)")
        Integer ttlMensajesMs,

        @Positive(message = "El maximo de mensajes debe ser mayor que 0")
        @Max(value = 1_000_000, message = "El maximo de mensajes no puede superar 1000000")
        Integer maxMensajes
) {

    public boolean esDurable() {
        return durable == null || durable;
    }

    public boolean esAutoDelete() {
        return autoDelete != null && autoDelete;
    }

    public TipoCola tipoEfectivo() {
        return tipo == null ? TipoCola.CLASSIC : tipo;
    }

    @AssertTrue(message = "Una cola QUORUM debe ser durable y no puede ser auto-delete")
    public boolean isConfiguracionQuorumValida() {
        return tipoEfectivo() != TipoCola.QUORUM || (esDurable() && !esAutoDelete());
    }

    @AssertTrue(message = "deadLetterRoutingKey requiere indicar tambien deadLetterExchange")
    public boolean isDeadLetterConsistente() {
        return deadLetterRoutingKey == null || deadLetterExchange != null;
    }
}
