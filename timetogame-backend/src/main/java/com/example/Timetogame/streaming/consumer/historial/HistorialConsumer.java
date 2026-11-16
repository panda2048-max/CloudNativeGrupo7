package com.example.Timetogame.streaming.consumer.historial;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.Timetogame.service.HistorialService;
import com.example.Timetogame.streaming.config.TopologiaKafka;
import com.example.Timetogame.streaming.consumer.EventoKafkaValidator;
import com.example.Timetogame.streaming.event.ActividadEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer group "historial": recibe TODOS los eventos de actividad, de forma
 * independiente al grupo "tendencias" (cada grupo lleva sus propios offsets).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistorialConsumer {

    private final EventoKafkaValidator validator;
    private final HistorialService historialService;

    @KafkaListener(id = "historial-actividad", topics = TopologiaKafka.ACTIVIDAD,
            groupId = TopologiaKafka.GRUPO_HISTORIAL,
            properties = "spring.json.value.default.type=com.example.Timetogame.streaming.event.ActividadEvent")
    public void onActividad(ConsumerRecord<String, ActividadEvent> record) {
        ActividadEvent evento = validator.validar(record);
        if (historialService.registrar(evento)) {
            log.info("Historial: {} vio el videojuego {} ({})", evento.usuario(), evento.videojuegoId(),
                    EventoKafkaValidator.ubicacion(record));
        }
    }
}
