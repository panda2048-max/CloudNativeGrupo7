package com.example.Timetogame.streaming.exception;

/**
 * Evento de Kafka que no cumple el contrato (campos faltantes o fuera de
 * rango). No se reintenta: va directo al DLT.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String message) {
        super(message);
    }
}
