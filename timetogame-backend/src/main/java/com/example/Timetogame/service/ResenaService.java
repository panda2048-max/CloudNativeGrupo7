package com.example.Timetogame.service;

import java.util.List;

import com.example.Timetogame.dto.CrearResenaRequest;
import com.example.Timetogame.dto.ResenaDTO;
import com.example.Timetogame.dto.ResumenResenasDTO;

public interface ResenaService {

    /**
     * Guarda la resena del usuario actual y la anuncia como actividad (Kafka),
     * para que aparezca en vivo en la portada.
     *
     * @throws com.example.Timetogame.exception.ResourceNotFoundException si el videojuego no existe
     */
    ResenaDTO crear(Long videojuegoId, CrearResenaRequest request);

    ResumenResenasDTO resumenDe(Long videojuegoId);

    /** Las ultimas resenas de todo el catalogo (estado inicial de la portada). */
    List<ResenaDTO> recientes();
}
