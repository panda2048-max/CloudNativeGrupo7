package com.example.Timetogame.streaming.producer;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.Timetogame.streaming.config.TopologiaKafka;
import com.example.Timetogame.streaming.event.ActividadEvent;

import lombok.RequiredArgsConstructor;

/**
 * Reenvia cada actividad (vista o resena) publicada como evento de Spring al
 * topico de actividad. Una resena se publica despues del commit (no se anuncia
 * una resena que termino en rollback); una vista no tiene transaccion y se
 * publica de inmediato (fallbackExecution).
 */
@Component
@RequiredArgsConstructor
public class ActividadProducer {

    private final KafkaEventoPublisher publisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onActividad(ActividadEvent evento) {
        publisher.publicar(TopologiaKafka.ACTIVIDAD, String.valueOf(evento.videojuegoId()), evento.eventId(),
                evento);
    }
}
