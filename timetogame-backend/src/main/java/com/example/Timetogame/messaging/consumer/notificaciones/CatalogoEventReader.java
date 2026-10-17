package com.example.Timetogame.messaging.consumer.notificaciones;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import com.example.Timetogame.event.CatalogoEvent;
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
public class CatalogoEventReader {

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public CatalogoEvent leer(Message message) {
        CatalogoEvent evento;
        try {
            evento = jsonMapper.readValue(message.getBody(), CatalogoEvent.class);
        } catch (JacksonException e) {
            throw new MensajeNoProcesableException("Payload JSON invalido: " + e.getOriginalMessage(), e);
        }

        if (evento == null) {
            throw new MensajeNoProcesableException("El mensaje no tiene contenido");
        }

        Set<ConstraintViolation<CatalogoEvent>> violaciones = validator.validate(evento);
        if (!violaciones.isEmpty()) {
            String detalle = violaciones.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new MensajeNoProcesableException("Evento invalido: " + detalle);
        }
        return evento;
    }
}
