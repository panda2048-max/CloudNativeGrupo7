package com.example.Timetogame.dto;

import java.util.List;

/** Resenas de un videojuego: promedio (null si no tiene), total y las mas recientes. */
public record ResumenResenasDTO(
        Double promedio,
        long total,
        List<ResenaDTO> resenas
) {
}
