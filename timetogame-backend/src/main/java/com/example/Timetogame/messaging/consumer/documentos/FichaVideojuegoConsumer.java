package com.example.Timetogame.messaging.consumer.documentos;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.Timetogame.event.SolicitudFicha;
import com.example.Timetogame.exception.ResourceNotFoundException;
import com.example.Timetogame.messaging.config.MensajeriaProperties;
import com.example.Timetogame.messaging.config.RabbitMQConfig;
import com.example.Timetogame.messaging.config.TopologiaMensajeria;
import com.example.Timetogame.messaging.consumer.support.MensajeJsonReader;
import com.example.Timetogame.messaging.consumer.support.ReintentoDiferidoHandler;
import com.example.Timetogame.messaging.exception.MensajeNoProcesableException;
import com.example.Timetogame.service.FichaVideojuegoService;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Genera (o elimina) la ficha tecnica de un videojuego.
 *
 * <p>Decision por resultado:
 * <ul>
 *   <li>Ficha generada/eliminada, o solicitud ya aplicada (duplicada): basicAck.</li>
 *   <li>Mensaje invalido, o el videojuego ya no existe: basicNack sin requeue
 *       -> DLQ inmediata (reintentar no cambiaria el resultado).</li>
 *   <li>Cualquier otro error (BD, conflicto de concurrencia): reintento
 *       diferido acotado y luego DLQ. Ver {@link ReintentoDiferidoHandler}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FichaVideojuegoConsumer {

    private final MensajeJsonReader mensajeJsonReader;
    private final FichaVideojuegoService fichaService;
    private final ReintentoDiferidoHandler reintentoHandler;
    private final MensajeriaProperties properties;

    @RabbitListener(id = "documentos-fichas", queues = TopologiaMensajeria.Documentos.FICHAS_QUEUE,
            containerFactory = RabbitMQConfig.MANUAL_ACK_CONTAINER_FACTORY)
    public void onSolicitudFicha(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            SolicitudFicha solicitud = mensajeJsonReader.leer(message, SolicitudFicha.class);
            switch (solicitud.accion()) {
                case GENERAR -> {
                    if (fichaService.generar(solicitud)) {
                        log.info("Ficha generada: solicitudId={}, videojuegoId={}", solicitud.solicitudId(),
                                solicitud.videojuegoId());
                    } else {
                        log.info("Solicitud {} ya aplicada (entrega duplicada); se confirma sin regenerar",
                                solicitud.solicitudId());
                    }
                }
                case ELIMINAR -> {
                    fichaService.eliminar(solicitud);
                    log.info("Ficha eliminada: videojuegoId={}", solicitud.videojuegoId());
                }
            }
            channel.basicAck(deliveryTag, false);
        } catch (MensajeNoProcesableException | ResourceNotFoundException e) {
            log.error("Solicitud de ficha {} no procesable, se envia a la DLQ sin reintentar: {}", messageId,
                    e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (RuntimeException e) {
            reintentoHandler.reintentarOEnviarADlq(message, channel, e, properties.politicaDocumentos());
        }
    }
}
