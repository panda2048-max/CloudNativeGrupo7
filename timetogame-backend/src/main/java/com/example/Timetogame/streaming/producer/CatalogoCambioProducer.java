package com.example.Timetogame.streaming.producer;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.Timetogame.event.CatalogoEvent;
import com.example.Timetogame.event.EntidadCatalogo;
import com.example.Timetogame.streaming.config.TopologiaKafka;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;

import lombok.RequiredArgsConstructor;

/**
 * Escucha el mismo CatalogoEvent que ya viaja por RabbitMQ (notificaciones) y
 * lo publica tambien en Kafka para los consumidores de streaming. Los
 * servicios del catalogo no cambiaron: siguen publicando un solo evento de Spring.
 */
@Component
@RequiredArgsConstructor
public class CatalogoCambioProducer {

    private final KafkaEventoPublisher publisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCambioDeCatalogo(CatalogoEvent evento) {
        if (evento.entidad() != EntidadCatalogo.VIDEOJUEGO) {
            return;
        }
        CatalogoCambioEvent cambio = new CatalogoCambioEvent(evento.eventId(), evento.ocurridoEn(), evento.tipo(),
                evento.entidadId(), evento.nombre(), evento.usuario());
        publisher.publicar(TopologiaKafka.CATALOGO_CAMBIOS, String.valueOf(evento.entidadId()), evento.eventId(),
                cambio);
    }
}
