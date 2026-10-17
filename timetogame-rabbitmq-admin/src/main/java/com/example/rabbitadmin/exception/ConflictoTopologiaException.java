package com.example.rabbitadmin.exception;

/**
 * 409: la operacion choca con el estado actual del broker (el recurso ya
 * existe, esta protegido, o el broker la rechazo con PRECONDITION_FAILED,
 * por ejemplo borrar una cola con mensajes usando soloSiVacia=true).
 */
public class ConflictoTopologiaException extends RuntimeException {

    public ConflictoTopologiaException(String message) {
        super(message);
    }
}
