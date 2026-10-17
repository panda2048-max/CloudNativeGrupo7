package com.example.Timetogame.messaging.consumer.notificaciones;

import static com.example.Timetogame.messaging.config.NotificacionesTopologia.DLQ_QUEUE;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.HEADER_ROUTING_KEY_ORIGINAL;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.HEADER_ULTIMO_ERROR;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.Timetogame.dto.MensajeFallidoDTO;
import com.example.Timetogame.messaging.config.RabbitMQConfig;
import com.example.Timetogame.service.MensajeFallidoService;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consume la DLQ de notificaciones: deja cada mensaje muerto en el log (con la
 * causa informada por el broker en x-death) y lo persiste para poder
 * consultarlo/reprocesarlo despues desde /api/mensajes-fallidos.
 *
 * <p>Si persistir falla, el mensaje vuelve a la DLQ con basicNack requeue=true
 * tras una pausa: la DLQ no tiene a su vez dead-letter, asi que rechazarlo sin
 * requeue lo perderia. La pausa evita un bucle caliente mientras la BD no responda.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionDlqConsumer {

    static final Duration ESPERA_ANTES_DE_REENCOLAR = Duration.ofSeconds(5);
    private static final int MAX_LARGO_PAYLOAD = 10_000;

    private final MensajeFallidoService mensajeFallidoService;

    @RabbitListener(id = "notificaciones-dlq", queues = DLQ_QUEUE,
            containerFactory = RabbitMQConfig.MANUAL_ACK_CONTAINER_FACTORY)
    public void onMensajeFallido(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        MensajeFallidoDTO registro = extraer(message);

        log.error("[DLQ] Mensaje no entregado: messageId={}, colaOrigen={}, routingKey={}, motivo={}, reintentos={}, "
                        + "ultimoError={}, payload={}", registro.messageId(), registro.colaOrigen(),
                registro.routingKey(), registro.motivo(), registro.reintentos(), registro.ultimoError(),
                registro.payload());

        try {
            mensajeFallidoService.registrar(registro);
            channel.basicAck(props.getDeliveryTag(), false);
        } catch (RuntimeException e) {
            log.error("[DLQ] No se pudo persistir el mensaje {}; vuelve a la DLQ en {}", registro.messageId(),
                    ESPERA_ANTES_DE_REENCOLAR, e);
            esperar();
            channel.basicNack(props.getDeliveryTag(), false, true);
        }
    }

    private MensajeFallidoDTO extraer(Message message) {
        MessageProperties props = message.getMessageProperties();
        Map<String, ?> ultimaMuerte = ultimaEntradaXDeath(props);

        String colaOrigen = ultimaMuerte != null ? Objects.toString(ultimaMuerte.get("queue"), null) : null;
        String motivo = ultimaMuerte != null ? Objects.toString(ultimaMuerte.get("reason"), "desconocido")
                : "desconocido";
        Object routingKeyOriginal = props.getHeader(HEADER_ROUTING_KEY_ORIGINAL);
        String routingKey = routingKeyOriginal != null ? routingKeyOriginal.toString() : primeraRoutingKey(ultimaMuerte);
        Object ultimoError = props.getHeader(HEADER_ULTIMO_ERROR);

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        if (payload.length() > MAX_LARGO_PAYLOAD) {
            payload = payload.substring(0, MAX_LARGO_PAYLOAD);
        }

        return new MensajeFallidoDTO(null, props.getMessageId(), colaOrigen, routingKey, motivo,
                NotificacionesReintentoHandler.reintentosPrevios(props),
                ultimoError != null ? ultimoError.toString() : null, payload, null);
    }

    /** x-death viene ordenado del evento mas reciente al mas antiguo. */
    private static Map<String, ?> ultimaEntradaXDeath(MessageProperties props) {
        List<Map<String, ?>> xDeath = props.getXDeathHeader();
        return xDeath == null || xDeath.isEmpty() ? null : xDeath.getFirst();
    }

    private static String primeraRoutingKey(Map<String, ?> entradaXDeath) {
        if (entradaXDeath != null && entradaXDeath.get("routing-keys") instanceof List<?> keys && !keys.isEmpty()) {
            return String.valueOf(keys.getFirst());
        }
        return null;
    }

    private static void esperar() {
        try {
            Thread.sleep(ESPERA_ANTES_DE_REENCOLAR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
