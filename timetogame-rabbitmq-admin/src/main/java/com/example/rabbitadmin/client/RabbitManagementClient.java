package com.example.rabbitadmin.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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

    private final List<RestClient> nodos;
    private final String vhost;

    public RabbitManagementClient(RabbitAdminProperties adminProperties, RabbitProperties rabbitProperties) {
        this.vhost = rabbitProperties.getVirtualHost() == null ? "/" : rabbitProperties.getVirtualHost();
        this.nodos = adminProperties.managementUrls().stream()
                .map(url -> RestClient.builder()
                        .baseUrl(url + "/api")
                        .defaultHeaders(headers -> headers.setBasicAuth(rabbitProperties.getUsername(),
                                rabbitProperties.getPassword()))
                        .build())
                .toList();
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
        List<T> resultado = conFailover(nodo -> nodo.get().uri(uri, variables).retrieve().body(tipo));
        return resultado == null ? List.of() : resultado;
    }

    private <T> Optional<T> obtenerOpcional(String uri, Class<T> tipo, Object... variables) {
        try {
            return Optional.ofNullable(conFailover(nodo -> nodo.get().uri(uri, variables).retrieve().body(tipo)));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    /**
     * Prueba los nodos en orden y usa el primero que responda. Solo un error de
     * red pasa al siguiente: un 404 o 401 es una respuesta valida del cluster.
     */
    private <T> T conFailover(Function<RestClient, T> consulta) {
        ResourceAccessException ultimoError = null;
        for (RestClient nodo : nodos) {
            try {
                return consulta.apply(nodo);
            } catch (ResourceAccessException e) {
                ultimoError = e;
            }
        }
        throw new BrokerNoDisponibleException("No se pudo contactar la API de management de ningun nodo de RabbitMQ",
                ultimoError);
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
