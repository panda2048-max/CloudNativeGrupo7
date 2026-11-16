package com.example.Timetogame.dto;

import java.util.Map;

/** Resultado de POST /api/streaming/simulacion: cuantas vistas se encolaron por juego. */
public record SimulacionDTO(
        int vistasGeneradas,
        Map<String, Integer> porVideojuego
) {
}
