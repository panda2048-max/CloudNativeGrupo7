package com.example.Timetogame.streaming.consumer;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import com.example.Timetogame.streaming.exception.EventoInvalidoException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

/**
 * Valida el payload ya deserializado contra las anotaciones del DTO. Un
 * evento que no cumple el contrato lanza EventoInvalidoException: el error
 * handler no lo reintenta y lo manda al DLT.
 */
@Component
@RequiredArgsConstructor
public class EventoKafkaValidator {

    private final Validator validator;

    public <T> T validar(ConsumerRecord<String, T> record) {
        T evento = record.value();
        if (evento == null) {
            throw new EventoInvalidoException("Mensaje sin contenido en " + ubicacion(record));
        }
        Set<ConstraintViolation<T>> violaciones = validator.validate(evento);
        if (!violaciones.isEmpty()) {
            String detalle = violaciones.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new EventoInvalidoException("Evento invalido en %s: %s".formatted(ubicacion(record), detalle));
        }
        return evento;
    }

    /** topico-particion@offset (key), para que cada log diga exactamente de que mensaje habla. */
    public static String ubicacion(ConsumerRecord<?, ?> record) {
        return "%s-%d@%d (key=%s)".formatted(record.topic(), record.partition(), record.offset(), record.key());
    }
}
