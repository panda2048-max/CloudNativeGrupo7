package com.example.Timetogame.service;

import java.util.List;

import com.example.Timetogame.dto.HistorialVistaDTO;
import com.example.Timetogame.streaming.event.VideojuegoVistoEvent;

/** Historial de videojuegos vistos por cada usuario con sesion. */
public interface HistorialService {

    /**
     * Guarda la vista si corresponde a un usuario real (no visitantes ni simulacion).
     *
     * @return false si no se guardo: no aplica o ya estaba registrada (entrega duplicada)
     */
    boolean registrar(VideojuegoVistoEvent evento);

    /** Los ultimos videojuegos que vio el usuario actual, del mas reciente al mas antiguo. */
    List<HistorialVistaDTO> recientesDelUsuarioActual();
}
