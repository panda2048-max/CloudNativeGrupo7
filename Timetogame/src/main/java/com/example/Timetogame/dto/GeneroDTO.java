package com.example.Timetogame.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneroDTO(
        Long id,

        @NotBlank(message = "El nombre del genero es obligatorio")
        @Size(max = 60, message = "El nombre del genero no puede superar 60 caracteres")
        String nombre
) {
}
