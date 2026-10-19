package com.example.Timetogame.service;

import com.example.Timetogame.dto.FichaVideojuegoDTO;
import com.example.Timetogame.dto.SolicitudFichaDTO;
import com.example.Timetogame.event.SolicitudFicha;

public interface FichaVideojuegoService {

    /** Encola la (re)generacion de la ficha; lanza ResourceNotFoundException si el videojuego no existe. */
    SolicitudFichaDTO solicitarGeneracion(Long videojuegoId);

    /**
     * Genera o regenera la ficha a partir del estado actual del videojuego.
     *
     * @return false si la solicitud ya se habia aplicado (entrega duplicada)
     * @throws com.example.Timetogame.exception.ResourceNotFoundException si el videojuego ya no existe
     */
    boolean generar(SolicitudFicha solicitud);

    /** Idempotente: eliminar una ficha inexistente no es error. */
    void eliminar(SolicitudFicha solicitud);

    FichaVideojuegoDTO findByVideojuegoId(Long videojuegoId);
}
