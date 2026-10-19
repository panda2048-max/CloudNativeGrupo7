package com.example.Timetogame.messaging.consumer.notificaciones;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.Timetogame.messaging.config.RabbitMQConfig;
import com.example.Timetogame.messaging.config.TopologiaMensajeria;
import com.example.Timetogame.messaging.consumer.support.RegistroDlqHandler;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;

/** Consume la DLQ de notificaciones: log + persistencia del mensaje muerto (ver {@link RegistroDlqHandler}). */
@Component
@RequiredArgsConstructor
public class NotificacionDlqConsumer {

    private final RegistroDlqHandler registroDlqHandler;

    @RabbitListener(id = "notificaciones-dlq", queues = TopologiaMensajeria.Notificaciones.DLQ_QUEUE,
            containerFactory = RabbitMQConfig.MANUAL_ACK_CONTAINER_FACTORY)
    public void onMensajeFallido(Message message, Channel channel) throws IOException {
        registroDlqHandler.registrar(message, channel);
    }
}
