package com.example.Timetogame.messaging.producer;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.Timetogame.event.CatalogoEvent;
import com.example.Timetogame.messaging.config.TopologiaMensajeria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Puente entre los eventos de aplicacion del catalogo y RabbitMQ. Los
 * servicios de negocio no conocen este componente: publican un
 * {@link CatalogoEvent} y este productor lo envia al broker recien cuando la
 * transaccion confirma, para no anunciar cambios que terminaron en rollback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(CatalogoEvent evento) {
        String routingKey = TopologiaMensajeria.Catalogo.routingKeyPara(evento);
        try {
            rabbitTemplate.convertAndSend(TopologiaMensajeria.Catalogo.EVENTS_EXCHANGE, routingKey, evento,
                    message -> {
                        message.getMessageProperties().setMessageId(evento.eventId());
                        return message;
                    },
                    new CorrelationData(evento.eventId()));
            log.info("Evento publicado: eventId={}, routingKey={}", evento.eventId(), routingKey);
        } catch (AmqpException e) {
            // El cambio en el catalogo ya esta confirmado en la BD: una caida del
            // broker no debe convertir una operacion exitosa en un error para el usuario.
            log.error("No se pudo publicar el evento {} ({}): broker no disponible", evento.eventId(),
                    routingKey, e);
        }
    }
}
