package com.example.rabbitadmin.exception;

/** 503: no hay conexion con RabbitMQ (AMQP o API de management). */
public class BrokerNoDisponibleException extends RuntimeException {

    public BrokerNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
