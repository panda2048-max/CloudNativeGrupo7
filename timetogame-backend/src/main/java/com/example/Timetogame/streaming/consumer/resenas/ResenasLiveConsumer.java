package com.example.Timetogame.streaming.consumer.resenas;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.Timetogame.service.AvisosEnVivoService;
import com.example.Timetogame.streaming.config.TopologiaKafka;
import com.example.Timetogame.streaming.consumer.EventoKafkaValidator;
import com.example.Timetogame.streaming.event.ActividadEvent;
import com.example.Timetogame.streaming.event.TipoActividad;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer group "resenas-live": del mismo topico de actividad toma solo las
 * resenas y las empuja por SSE a la portada. Lee desde el final del topico
 * (auto-offset-reset=latest): al arrancar no tiene sentido re-avisar resenas
 * viejas, el estado inicial de la portada sale de la BD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResenasLiveConsumer {

    private final EventoKafkaValidator validator;
    private final AvisosEnVivoService avisosEnVivoService;

    @KafkaListener(id = "resenas-live", topics = TopologiaKafka.ACTIVIDAD,
            groupId = TopologiaKafka.GRUPO_RESENAS_LIVE,
            properties = {
                    "spring.json.value.default.type=com.example.Timetogame.streaming.event.ActividadEvent",
                    "auto.offset.reset=latest"})
    public void onActividad(ConsumerRecord<String, ActividadEvent> record) {
        ActividadEvent evento = validator.validar(record);
        if (evento.tipo() != TipoActividad.RESENA) {
            return;
        }
        avisosEnVivoService.notificarResena(evento);
        log.info("Resena en vivo: {} califico {} con {} estrellas ({})", evento.usuario(), evento.videojuegoId(),
                evento.calificacion(), EventoKafkaValidator.ubicacion(record));
    }
}
