package com.example.rabbitadmin.messaging.consumer.auditoria;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import com.example.rabbitadmin.event.OperacionTopologiaEvent;
import com.example.rabbitadmin.messaging.exception.MensajeNoProcesableException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Deserializa y valida el payload dentro del try/catch del consumidor. */
@Component
@RequiredArgsConstructor
public class OperacionTopologiaEventReader {

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public OperacionTopologiaEvent leer(Message message) {
        OperacionTopologiaEvent evento;
        try {
            evento = jsonMapper.readValue(message.getBody(), OperacionTopologiaEvent.class);
        } catch (JacksonException e) {
            throw new MensajeNoProcesableException("Payload JSON invalido: " + e.getOriginalMessage(), e);
        }

        if (evento == null) {
            throw new MensajeNoProcesableException("El mensaje no tiene contenido");
        }

        Set<ConstraintViolation<OperacionTopologiaEvent>> violaciones = validator.validate(evento);
        if (!violaciones.isEmpty()) {
            String detalle = violaciones.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new MensajeNoProcesableException("Evento de auditoria invalido: " + detalle);
        }
        return evento;
    }
}
