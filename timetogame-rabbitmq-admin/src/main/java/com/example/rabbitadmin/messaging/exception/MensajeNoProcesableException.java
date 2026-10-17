package com.example.rabbitadmin.messaging.exception;

/** Error NO recuperable: el mensaje en si esta mal; va directo a la DLQ. */
public class MensajeNoProcesableException extends RuntimeException {

    public MensajeNoProcesableException(String message) {
        super(message);
    }

    public MensajeNoProcesableException(String message, Throwable cause) {
        super(message, cause);
    }
}
