package com.example.rabbitadmin.dto;

import static com.example.rabbitadmin.dto.ReglasNombre.MENSAJE_NOMBRE;
import static com.example.rabbitadmin.dto.ReglasNombre.PATRON_NOMBRE;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CrearExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = PATRON_NOMBRE, message = "Nombre de exchange invalido: " + MENSAJE_NOMBRE)
        String nombre,

        @NotNull(message = "El tipo de exchange es obligatorio: DIRECT, TOPIC, FANOUT o HEADERS")
        TipoExchange tipo,

        /** Por defecto true. */
        Boolean durable,

        /** Por defecto false. */
        Boolean autoDelete
) {

    public boolean esDurable() {
        return durable == null || durable;
    }

    public boolean esAutoDelete() {
        return autoDelete != null && autoDelete;
    }
}
