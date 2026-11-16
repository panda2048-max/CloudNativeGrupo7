package com.example.Timetogame.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** El usuario y el juego no vienen en el cuerpo: salen del token y de la URL. */
public record CrearResenaRequest(
        @NotNull(message = "La calificacion es obligatoria")
        @Min(value = 1, message = "La calificacion minima es 1 estrella")
        @Max(value = 5, message = "La calificacion maxima es 5 estrellas")
        Integer calificacion,

        @NotBlank(message = "El comentario es obligatorio")
        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comentario
) {
}
