package com.example.rabbitadmin.service;

import java.util.List;

import com.example.rabbitadmin.dto.BindingResponse;
import com.example.rabbitadmin.dto.ColaResponse;
import com.example.rabbitadmin.dto.CrearBindingRequest;
import com.example.rabbitadmin.dto.CrearColaRequest;
import com.example.rabbitadmin.dto.CrearExchangeRequest;
import com.example.rabbitadmin.dto.ExchangeResponse;

/**
 * Operaciones de alto nivel sobre la topologia del broker. Los controladores
 * solo conocen esta interfaz; los detalles de RabbitMQ (AmqpAdmin, API de
 * management, codigos de error AMQP) quedan en la implementacion.
 */
public interface RabbitAdminService {

    List<ColaResponse> listarColas();

    ColaResponse obtenerCola(String nombre);

    ColaResponse crearCola(CrearColaRequest request);

    /**
     * @param soloSiSinConsumidores falla con 409 si la cola tiene consumidores conectados
     * @param soloSiVacia           falla con 409 si la cola tiene mensajes pendientes
     */
    void eliminarCola(String nombre, boolean soloSiSinConsumidores, boolean soloSiVacia);

    List<ExchangeResponse> listarExchanges();

    ExchangeResponse obtenerExchange(String nombre);

    ExchangeResponse crearExchange(CrearExchangeRequest request);

    /** @param soloSiSinBindings falla con 409 si el exchange todavia tiene bindings */
    void eliminarExchange(String nombre, boolean soloSiSinBindings);

    List<BindingResponse> listarBindings();

    BindingResponse crearBinding(CrearBindingRequest request);

    void eliminarBinding(String exchange, String cola, String routingKey);
}
