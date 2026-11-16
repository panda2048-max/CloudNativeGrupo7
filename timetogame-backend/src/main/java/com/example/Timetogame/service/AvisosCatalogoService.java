package com.example.Timetogame.service;

import com.example.Timetogame.streaming.event.CatalogoCambioEvent;

/** Avisa en vivo a los navegadores conectados que el catalogo cambio. */
public interface AvisosCatalogoService {

    void notificarCambio(CatalogoCambioEvent evento);
}
