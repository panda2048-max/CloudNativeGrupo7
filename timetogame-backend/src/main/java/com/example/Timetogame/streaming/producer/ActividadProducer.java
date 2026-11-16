package com.example.Timetogame.streaming.producer;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.Timetogame.streaming.config.TopologiaKafka;
import com.example.Timetogame.streaming.event.VideojuegoVistoEvent;

import lombok.RequiredArgsConstructor;

/** Reenvia cada vista publicada por ActividadService al topico de actividad. */
@Component
@RequiredArgsConstructor
public class ActividadProducer {

    private final KafkaEventoPublisher publisher;

    @Async
    @EventListener
    public void onVideojuegoVisto(VideojuegoVistoEvent evento) {
        publisher.publicar(TopologiaKafka.ACTIVIDAD, String.valueOf(evento.videojuegoId()), evento.eventId(),
                evento);
    }
}
