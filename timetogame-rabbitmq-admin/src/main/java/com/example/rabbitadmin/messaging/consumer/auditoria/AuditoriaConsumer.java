package com.example.rabbitadmin.messaging.consumer.auditoria;

import static com.example.rabbitadmin.messaging.config.AuditoriaTopologia.AUDITORIA_QUEUE;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.rabbitadmin.event.OperacionTopologiaEvent;
import com.example.rabbitadmin.messaging.config.RabbitMQConfig;
import com.example.rabbitadmin.messaging.exception.MensajeNoProcesableException;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra en el log de auditoria (logs/auditoria-topologia.log, ver
 * logback-spring.xml) cada cambio hecho sobre la topologia.
 *
 * <p>Decision por resultado:
 * <ul>
 *   <li>Registrado: basicAck.</li>
 *   <li>Payload invalido: basicNack sin requeue -> DLQ.</li>
 *   <li>Cualquier otro fallo: basicNack sin requeue -> DLQ. No se reintenta
 *       porque escribir el log no depende de recursos externos: el fallo no es
 *       transitorio, y reencolar solo generaria un bucle.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaConsumer {

    private static final Logger AUDITORIA = LoggerFactory.getLogger("AUDITORIA_TOPOLOGIA");

    private final OperacionTopologiaEventReader reader;

    @RabbitListener(id = "auditoria-registro", queues = AUDITORIA_QUEUE,
            containerFactory = RabbitMQConfig.MANUAL_ACK_CONTAINER_FACTORY)
    public void onOperacion(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            OperacionTopologiaEvent evento = reader.leer(message);
            AUDITORIA.info("{} {} | {} | usuario={} | ocurridoEn={} | eventId={}", evento.operacion(),
                    evento.recurso(), evento.descripcion(), evento.usuario(), evento.ocurridoEn(), evento.eventId());
            channel.basicAck(deliveryTag, false);
        } catch (MensajeNoProcesableException e) {
            log.error("Evento de auditoria {} no procesable, se envia a la DLQ: {}", messageId, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (RuntimeException e) {
            log.error("Fallo inesperado registrando la auditoria {}, se envia a la DLQ", messageId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
