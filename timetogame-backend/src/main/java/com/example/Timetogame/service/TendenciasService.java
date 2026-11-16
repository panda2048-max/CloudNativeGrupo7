package com.example.Timetogame.service;

import java.util.List;

import com.example.Timetogame.dto.TendenciaDTO;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;
import com.example.Timetogame.streaming.event.ActividadEvent;

/** Ranking en vivo de los videojuegos mas vistos en una ventana deslizante. */
public interface TendenciasService {

    /** @return false si la vista ya se habia contado (entrega duplicada) o quedo fuera de la ventana */
    boolean registrarVista(ActividadEvent evento);

    /** Mantiene el ranking coherente con el catalogo: titulo nuevo o juego eliminado. */
    void aplicarCambio(CatalogoCambioEvent evento);

    List<TendenciaDTO> ranking();

    /** Indica si el ranking cambio desde la ultima llamada (para no emitir de mas por SSE). */
    boolean consumirCambios();
}
