package com.example.Timetogame.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VideojuegoDTO(
        Long id,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 120, message = "El titulo no puede superar 120 caracteres")
        String titulo,

        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal precio,

        LocalDate fechaLanzamiento,

        @NotNull(message = "El genero es obligatorio")
        Long generoId,

        String generoNombre
) {
}
