package com.example.Timetogame.service;

import java.util.List;

import com.example.Timetogame.dto.NotificacionDTO;
import com.example.Timetogame.event.CatalogoEvent;

public interface NotificacionService {

    /**
     * Registra la notificacion de un evento de catalogo.
     *
     * @return false si el evento ya habia sido procesado (redelivery duplicada)
     */
    boolean registrar(CatalogoEvent evento);

    List<NotificacionDTO> findRecientes();
}
