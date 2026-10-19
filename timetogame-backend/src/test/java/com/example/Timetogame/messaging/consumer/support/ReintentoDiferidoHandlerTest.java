package com.example.Timetogame.messaging.consumer.support;

import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Headers.REINTENTOS;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Headers.ROUTING_KEY_ORIGINAL;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Headers.ULTIMO_ERROR;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Notificaciones.RETRY_EXCHANGE;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Notificaciones.RETRY_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.example.Timetogame.messaging.config.PoliticaReintento;
import com.rabbitmq.client.Channel;

class ReintentoDiferidoHandlerTest {

    private static final long DELIVERY_TAG = 7L;

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final Channel channel = mock(Channel.class);
    private static final PoliticaReintento POLITICA =
            new PoliticaReintento(RETRY_EXCHANGE, RETRY_ROUTING_KEY, 3, Duration.ofSeconds(5));

    private final ReintentoDiferidoHandler handler = new ReintentoDiferidoHandler(rabbitTemplate);

    @Test
    void programaElPrimerReintentoYConfirmaElOriginal() throws Exception {
        handler.reintentarOEnviarADlq(mensaje(null), channel, new IllegalStateException("BD caida"), POLITICA);

        ArgumentCaptor<Message> copia = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RETRY_EXCHANGE), eq(RETRY_ROUTING_KEY), copia.capture());
        MessageProperties props = copia.getValue().getMessageProperties();
        assertThat(props.<Integer>getHeader(REINTENTOS)).isEqualTo(1);
        assertThat(props.<String>getHeader(ULTIMO_ERROR)).contains("BD caida");
        assertThat(props.<String>getHeader(ROUTING_KEY_ORIGINAL)).isEqualTo("catalogo.genero.creado");

        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void incrementaElContadorEnReintentosSucesivos() throws Exception {
        handler.reintentarOEnviarADlq(mensaje(2), channel, new IllegalStateException("timeout"), POLITICA);

        ArgumentCaptor<Message> copia = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RETRY_EXCHANGE), eq(RETRY_ROUTING_KEY), copia.capture());
        assertThat(copia.getValue().getMessageProperties().<Integer>getHeader(REINTENTOS)).isEqualTo(3);
    }

    @Test
    void enviaALaDlqConNackCuandoSeAgotanLosReintentos() throws Exception {
        handler.reintentarOEnviarADlq(mensaje(3), channel, new IllegalStateException("BD caida"), POLITICA);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void enviaALaDlqSiNoSePuedeProgramarElReintento() throws Exception {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        handler.reintentarOEnviarADlq(mensaje(0), channel, new IllegalStateException("BD caida"), POLITICA);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private static Message mensaje(Integer reintentosPrevios) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(DELIVERY_TAG);
        props.setMessageId("e-1");
        props.setReceivedRoutingKey("catalogo.genero.creado");
        if (reintentosPrevios != null) {
            props.setHeader(REINTENTOS, reintentosPrevios);
        }
        return new Message("{}".getBytes(), props);
    }
}
