package com.example.Timetogame.messaging.exception;

/**
 * Error NO recuperable: el mensaje en si esta mal (JSON invalido, campos
 * faltantes o fuera de rango). Reintentarlo daria el mismo resultado, asi que
 * va directo a la DLQ con basicNack sin requeue.
 */
public class MensajeNoProcesableException extends RuntimeException {

    public MensajeNoProcesableException(String message) {
        super(message);
    }

    public MensajeNoProcesableException(String message, Throwable cause) {
        super(message, cause);
    }
}
