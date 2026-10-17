package com.example.rabbitadmin.exception;

/** 404: la cola, exchange o binding no existe en el broker. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
