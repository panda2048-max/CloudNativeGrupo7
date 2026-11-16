package com.example.Timetogame.service;

import com.example.Timetogame.dto.SimulacionDTO;
import com.example.Timetogame.dto.VideojuegoDTO;

/** Registro de la actividad de los usuarios sobre el catalogo. */
public interface ActividadService {

    /** Anota que el usuario actual (o un visitante) abrio la ficha del videojuego. */
    void registrarVista(VideojuegoDTO videojuego);

    /**
     * Genera vistas sinteticas repartidas entre los videojuegos existentes
     * (unos pocos concentran la mayoria), para la demo de carga y lag.
     */
    SimulacionDTO simularVistas(int cantidad);
}
