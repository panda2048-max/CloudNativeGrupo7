package com.example.rabbitadmin.messaging.producer;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.rabbitadmin.event.OperacionTopologiaEvent;
import com.example.rabbitadmin.messaging.config.AuditoriaTopologia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reenvia a RabbitMQ los eventos de auditoria que publica RabbitAdminService.
 * El servicio no conoce este componente ni el exchange de destino.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @EventListener
    public void publicar(OperacionTopologiaEvent evento) {
        String routingKey = AuditoriaTopologia.routingKeyPara(evento);
        try {
            rabbitTemplate.convertAndSend(AuditoriaTopologia.AUDITORIA_EXCHANGE, routingKey, evento,
                    message -> {
                        message.getMessageProperties().setMessageId(evento.eventId());
                        return message;
                    },
                    new CorrelationData(evento.eventId()));
            log.info("Evento de auditoria publicado: eventId={}, routingKey={}", evento.eventId(), routingKey);
        } catch (AmqpException e) {
            // La operacion sobre el broker ya se realizo; que falle la auditoria no debe revertirla.
            log.error("No se pudo publicar el evento de auditoria {} ({})", evento.eventId(), routingKey, e);
        }
    }
}
