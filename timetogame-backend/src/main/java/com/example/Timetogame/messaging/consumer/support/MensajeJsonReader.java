package com.example.Timetogame.messaging.consumer.support;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import com.example.Timetogame.messaging.exception.MensajeNoProcesableException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Convierte y valida el cuerpo del mensaje dentro del try/catch del
 * consumidor (el contenedor entrega el cuerpo crudo, ver
 * RabbitMQConfig.CuerpoCrudoMessageConverter), asi cualquier payload invalido
 * termina en el log y el nack explicitos del consumidor.
 */
@Component
@RequiredArgsConstructor
public class MensajeJsonReader {

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public <T> T leer(Message message, Class<T> tipo) {
        T contenido;
        try {
            contenido = jsonMapper.readValue(message.getBody(), tipo);
        } catch (JacksonException e) {
            throw new MensajeNoProcesableException("Payload JSON invalido: " + e.getOriginalMessage(), e);
        }

        if (contenido == null) {
            throw new MensajeNoProcesableException("El mensaje no tiene contenido");
        }

        Set<ConstraintViolation<T>> violaciones = validator.validate(contenido);
        if (!violaciones.isEmpty()) {
            String detalle = violaciones.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new MensajeNoProcesableException("Mensaje invalido (" + tipo.getSimpleName() + "): " + detalle);
        }
        return contenido;
    }
}
