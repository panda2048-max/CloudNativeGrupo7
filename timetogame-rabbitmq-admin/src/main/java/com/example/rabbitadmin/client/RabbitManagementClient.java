package com.example.rabbitadmin.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.example.rabbitadmin.config.RabbitAdminProperties;
import com.example.rabbitadmin.exception.BrokerNoDisponibleException;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lectura de la topologia via la API HTTP de management. El protocolo AMQP
 * permite declarar y borrar recursos pero no listarlos, por eso las consultas
 * van por aqui y las escrituras por AmqpAdmin (ver RabbitAdminServiceImpl).
 */
@Component
public class RabbitManagementClient {

    private final RestClient restClient;
    private final String vhost;

    public RabbitManagementClient(RabbitAdminProperties adminProperties, RabbitProperties rabbitProperties) {
        this.vhost = rabbitProperties.getVirtualHost() == null ? "/" : rabbitProperties.getVirtualHost();
        this.restClient = RestClient.builder()
                .baseUrl(adminProperties.managementUrl() + "/api")
                .defaultHeaders(headers -> headers.setBasicAuth(rabbitProperties.getUsername(),
                        rabbitProperties.getPassword()))
                .build();
    }

    public List<ColaInfo> listarColas() {
        return obtenerLista("/queues/{vhost}", new ParameterizedTypeReference<>() { }, vhost);
    }

    public Optional<ColaInfo> buscarCola(String nombre) {
        return obtenerOpcional("/queues/{vhost}/{nombre}", ColaInfo.class, vhost, nombre);
    }

    public List<ExchangeInfo> listarExchanges() {
        return obtenerLista("/exchanges/{vhost}", new ParameterizedTypeReference<>() { }, vhost);
    }

    public Optional<ExchangeInfo> buscarExchange(String nombre) {
        return obtenerOpcional("/exchanges/{vhost}/{nombre}", ExchangeInfo.class, vhost, nombre);
    }

    public List<BindingInfo> listarBindings() {
        return obtenerLista("/bindings/{vhost}", new ParameterizedTypeReference<>() { }, vhost);
    }

    public List<BindingInfo> bindingsEntre(String exchange, String cola) {
        try {
            return obtenerLista("/bindings/{vhost}/e/{exchange}/q/{cola}", new ParameterizedTypeReference<>() { },
                    vhost, exchange, cola);
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        }
    }

    private <T> List<T> obtenerLista(String uri, ParameterizedTypeReference<List<T>> tipo, Object... variables) {
        try {
            List<T> resultado = restClient.get().uri(uri, variables).retrieve().body(tipo);
            return resultado == null ? List.of() : resultado;
        } catch (ResourceAccessException e) {
            throw new BrokerNoDisponibleException("No se pudo contactar la API de management de RabbitMQ", e);
        }
    }

    private <T> Optional<T> obtenerOpcional(String uri, Class<T> tipo, Object... variables) {
        try {
            return Optional.ofNullable(restClient.get().uri(uri, variables).retrieve().body(tipo));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (ResourceAccessException e) {
            throw new BrokerNoDisponibleException("No se pudo contactar la API de management de RabbitMQ", e);
        }
    }

    public record ColaInfo(
            String name,
            String type,
            boolean durable,
            @JsonProperty("auto_delete") boolean autoDelete,
            Map<String, Object> arguments,
            Long messages,
            Integer consumers) {
    }

    public record ExchangeInfo(
            String name,
            String type,
            boolean durable,
            @JsonProperty("auto_delete") boolean autoDelete) {
    }

    public record BindingInfo(
            String source,
            String destination,
            @JsonProperty("destination_type") String destinationType,
            @JsonProperty("routing_key") String routingKey) {
    }
}
