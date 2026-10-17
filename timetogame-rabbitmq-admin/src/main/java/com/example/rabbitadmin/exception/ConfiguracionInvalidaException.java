package com.example.rabbitadmin.exception;

/**
 * 400: la peticion es sintacticamente valida pero la combinacion no tiene
 * sentido contra el broker real (ej. dead letter exchange inexistente, o
 * comodines de topic en un exchange direct).
 */
public class ConfiguracionInvalidaException extends RuntimeException {

    public ConfiguracionInvalidaException(String message) {
        super(message);
    }
}
