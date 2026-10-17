package com.example.rabbitadmin.messaging.consumer.auditoria;

import static com.example.rabbitadmin.messaging.config.AuditoriaTopologia.AUDITORIA_DLQ;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.rabbitadmin.messaging.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;

import lombok.extern.slf4j.Slf4j;

/**
 * Consume la DLQ de auditoria y deja cada mensaje muerto en el log, con la
 * causa informada por el broker (x-death), para revisarlo manualmente.
 * Siempre confirma: el registro ya quedo en el log y esta DLQ no tiene a su
 * vez dead letter, asi que reencolar solo produciria un bucle.
 */
@Slf4j
@Component
public class AuditoriaDlqConsumer {

    private static final int MAX_LARGO_PAYLOAD = 2_000;

    @RabbitListener(id = "auditoria-dlq", queues = AUDITORIA_DLQ,
            containerFactory = RabbitMQConfig.MANUAL_ACK_CONTAINER_FACTORY)
    public void onMensajeFallido(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        Map<String, ?> ultimaMuerte = ultimaEntradaXDeath(props);

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        if (payload.length() > MAX_LARGO_PAYLOAD) {
            payload = payload.substring(0, MAX_LARGO_PAYLOAD) + "...";
        }

        log.error("[DLQ auditoria] Mensaje no entregado: messageId={}, colaOrigen={}, motivo={}, routingKeys={}, payload={}",
                props.getMessageId(),
                ultimaMuerte != null ? ultimaMuerte.get("queue") : null,
                ultimaMuerte != null ? ultimaMuerte.get("reason") : "desconocido",
                ultimaMuerte != null ? ultimaMuerte.get("routing-keys") : null,
                payload);

        channel.basicAck(props.getDeliveryTag(), false);
    }

    private static Map<String, ?> ultimaEntradaXDeath(MessageProperties props) {
        List<Map<String, ?>> xDeath = props.getXDeathHeader();
        return xDeath == null || xDeath.isEmpty() ? null : xDeath.getFirst();
    }
}
