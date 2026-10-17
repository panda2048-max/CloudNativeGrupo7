package com.example.rabbitadmin.dto;

import static com.example.rabbitadmin.dto.ReglasNombre.MENSAJE_NOMBRE;
import static com.example.rabbitadmin.dto.ReglasNombre.MENSAJE_ROUTING_KEY;
import static com.example.rabbitadmin.dto.ReglasNombre.PATRON_NOMBRE;
import static com.example.rabbitadmin.dto.ReglasNombre.PATRON_ROUTING_KEY;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Enlaza una cola a un exchange. El exchange por defecto ("") no es valido:
 * RabbitMQ no permite agregarle bindings (ya enlaza implicitamente cada cola
 * por su nombre).
 */
public record CrearBindingRequest(
        @NotBlank(message = "El exchange origen es obligatorio")
        @Pattern(regexp = PATRON_NOMBRE, message = "Nombre de exchange invalido: " + MENSAJE_NOMBRE)
        String exchange,

        @NotBlank(message = "La cola destino es obligatoria")
        @Pattern(regexp = PATRON_NOMBRE, message = "Nombre de cola invalido: " + MENSAJE_NOMBRE)
        String cola,

        /** Opcional (vacia por defecto, tipico en fanout). */
        @Pattern(regexp = PATRON_ROUTING_KEY, message = "Routing key invalida: " + MENSAJE_ROUTING_KEY)
        String routingKey
) {

    public String routingKeyEfectiva() {
        return routingKey == null ? "" : routingKey;
    }
}
