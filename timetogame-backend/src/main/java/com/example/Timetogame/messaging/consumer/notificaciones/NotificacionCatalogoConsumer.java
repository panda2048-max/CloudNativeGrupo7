package com.example.Timetogame.messaging.consumer.notificaciones;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.Timetogame.event.CatalogoEvent;
import com.example.Timetogame.messaging.config.MensajeriaProperties;
import com.example.Timetogame.messaging.config.RabbitMQConfig;
import com.example.Timetogame.messaging.config.TopologiaMensajeria;
import com.example.Timetogame.messaging.consumer.support.MensajeJsonReader;
import com.example.Timetogame.messaging.consumer.support.ReintentoDiferidoHandler;
import com.example.Timetogame.messaging.exception.MensajeNoProcesableException;
import com.example.Timetogame.service.NotificacionService;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consume los eventos de catalogo y los registra como notificaciones.
 *
 * <p>Decision por resultado (todas confirman o rechazan explicitamente):
 * <ul>
 *   <li>Procesado, o ya procesado antes (entrega duplicada): basicAck.</li>
 *   <li>Mensaje invalido ({@link MensajeNoProcesableException}): basicNack sin
 *       requeue -> DLQ inmediata, reintentarlo no lo arreglaria.</li>
 *   <li>Cualquier otro error (recuperable): reintento diferido acotado y, si se
 *       agotan los intentos, DLQ. Ver {@link ReintentoDiferidoHandler}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionCatalogoConsumer {

    private final MensajeJsonReader mensajeJsonReader;
    private final NotificacionService notificacionService;
    private final ReintentoDiferidoHandler reintentoHandler;
    private final MensajeriaProperties properties;

    @RabbitListener(id = "notificaciones-catalogo", queues = TopologiaMensajeria.Notificaciones.QUEUE,
            containerFactory = RabbitMQConfig.MANUAL_ACK_CONTAINER_FACTORY)
    public void onCatalogoEvent(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            CatalogoEvent evento = mensajeJsonReader.leer(message, CatalogoEvent.class);
            if (notificacionService.registrar(evento)) {
                log.info("Notificacion registrada: eventId={}, {} {} id={}", evento.eventId(), evento.entidad(),
                        evento.tipo(), evento.entidadId());
            } else {
                log.info("Evento {} ya procesado (entrega duplicada); se confirma sin reprocesar", evento.eventId());
            }
            channel.basicAck(deliveryTag, false);
        } catch (MensajeNoProcesableException e) {
            log.error("Mensaje {} no procesable, se envia a la DLQ sin reintentar: {}", messageId, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (RuntimeException e) {
            reintentoHandler.reintentarOEnviarADlq(message, channel, e, properties.politicaNotificaciones());
        }
    }
}
