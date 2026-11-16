package com.example.Timetogame.dto;

/** Un videojuego del ranking de tendencias y sus vistas dentro de la ventana. */
public record TendenciaDTO(
        Long videojuegoId,
        String titulo,
        long vistas
) {
}
