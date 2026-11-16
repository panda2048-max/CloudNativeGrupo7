package com.example.Timetogame.service;

import com.example.Timetogame.streaming.event.ActividadEvent;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;

/** Avisa en vivo (SSE) a los navegadores conectados lo que pasa en el catalogo. */
public interface AvisosEnVivoService {

    void notificarCambio(CatalogoCambioEvent evento);

    void notificarResena(ActividadEvent resena);
}
