package com.example.Timetogame.messaging.consumer.notificaciones;

import static com.example.Timetogame.messaging.config.NotificacionesTopologia.HEADER_REINTENTOS;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.HEADER_ROUTING_KEY_ORIGINAL;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.HEADER_ULTIMO_ERROR;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.RETRY_EXCHANGE;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.RETRY_ROUTING_KEY;

import java.io.IOException;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.Timetogame.messaging.config.NotificacionesProperties;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Politica para errores RECUPERABLES (BD caida, timeouts, fallas inesperadas):
 * <ul>
 *   <li>Quedan reintentos: se publica una copia en la cola de reintento (vuelve
 *       sola tras el TTL) y se hace basicAck del original.</li>
 *   <li>Reintentos agotados, o no se pudo programar el reintento: basicNack sin
 *       requeue -> el broker lo deriva a la DLQ.</li>
 * </ul>
 * Nunca se usa basicNack con requeue=true: reencolar al instante produce un
 * bucle caliente mientras la causa (ej. la BD) siga caida.
 * Copiar y luego confirmar no es atomico: si el proceso cae entre ambos pasos
 * el mensaje puede llegar dos veces, y por eso el registro es idempotente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionesReintentoHandler {

    private static final int MAX_LARGO_ERROR = 250;

    private final RabbitTemplate rabbitTemplate;
    private final NotificacionesProperties properties;

    public void reintentarOEnviarADlq(Message message, Channel channel, Exception causa) throws IOException {
        MessageProperties props = message.getMessageProperties();
        long deliveryTag = props.getDeliveryTag();
        int reintentosPrevios = reintentosPrevios(props);

        if (reintentosPrevios >= properties.maxReintentos()) {
            log.error("Reintentos agotados ({}/{}) para messageId={}; se envia a la DLQ. Causa: {}",
                    reintentosPrevios, properties.maxReintentos(), props.getMessageId(), causa.toString());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        Message copia = MessageBuilder.fromClonedMessage(message)
                .setHeader(HEADER_REINTENTOS, reintentosPrevios + 1)
                .setHeader(HEADER_ULTIMO_ERROR, resumir(causa))
                .setHeader(HEADER_ROUTING_KEY_ORIGINAL, routingKeyOriginal(props))
                .build();
        try {
            rabbitTemplate.send(RETRY_EXCHANGE, RETRY_ROUTING_KEY, copia);
        } catch (AmqpException e) {
            log.error("No se pudo programar el reintento de messageId={}; se envia a la DLQ", props.getMessageId(), e);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        channel.basicAck(deliveryTag, false);
        log.warn("Fallo recuperable en messageId={}; reintento {}/{} en {}. Causa: {}", props.getMessageId(),
                reintentosPrevios + 1, properties.maxReintentos(), properties.retrasoReintento(), causa.toString());
    }

    static int reintentosPrevios(MessageProperties props) {
        Object valor = props.getHeader(HEADER_REINTENTOS);
        return valor instanceof Number numero ? numero.intValue() : 0;
    }

    private static String routingKeyOriginal(MessageProperties props) {
        Object original = props.getHeader(HEADER_ROUTING_KEY_ORIGINAL);
        return original != null ? original.toString() : props.getReceivedRoutingKey();
    }

    private static String resumir(Exception causa) {
        String texto = causa.getClass().getSimpleName() + ": " + causa.getMessage();
        return texto.length() > MAX_LARGO_ERROR ? texto.substring(0, MAX_LARGO_ERROR) : texto;
    }
}
