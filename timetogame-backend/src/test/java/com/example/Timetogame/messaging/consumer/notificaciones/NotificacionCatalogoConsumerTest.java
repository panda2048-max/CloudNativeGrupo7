package com.example.Timetogame.messaging.consumer.notificaciones;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataAccessResourceFailureException;

import com.example.Timetogame.event.CatalogoEvent;
import com.example.Timetogame.event.EntidadCatalogo;
import com.example.Timetogame.event.TipoEventoCatalogo;
import com.example.Timetogame.messaging.exception.MensajeNoProcesableException;
import com.example.Timetogame.service.NotificacionService;
import com.rabbitmq.client.Channel;

class NotificacionCatalogoConsumerTest {

    private static final long DELIVERY_TAG = 42L;

    private final CatalogoEventReader reader = mock(CatalogoEventReader.class);
    private final NotificacionService notificacionService = mock(NotificacionService.class);
    private final NotificacionesReintentoHandler reintentoHandler = mock(NotificacionesReintentoHandler.class);
    private final Channel channel = mock(Channel.class);

    private NotificacionCatalogoConsumer consumer;
    private Message message;
    private CatalogoEvent evento;

    @BeforeEach
    void setUp() {
        consumer = new NotificacionCatalogoConsumer(reader, notificacionService, reintentoHandler);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(DELIVERY_TAG);
        message = new Message("{}".getBytes(), props);
        evento = new CatalogoEvent("e-1", TipoEventoCatalogo.CREADO, EntidadCatalogo.GENERO, 1L, "RPG",
                "admin@test", Instant.now());
    }

    @Test
    void confirmaConAckCuandoSeProcesaCorrectamente() throws Exception {
        when(reader.leer(message)).thenReturn(evento);
        when(notificacionService.registrar(evento)).thenReturn(true);

        consumer.onCatalogoEvent(message, channel);

        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void confirmaConAckUnaEntregaDuplicadaSinReprocesar() throws Exception {
        when(reader.leer(message)).thenReturn(evento);
        when(notificacionService.registrar(evento)).thenReturn(false);

        consumer.onCatalogoEvent(message, channel);

        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void enviaDirectoALaDlqUnMensajeNoProcesable() throws Exception {
        when(reader.leer(message)).thenThrow(new MensajeNoProcesableException("Payload JSON invalido"));

        consumer.onCatalogoEvent(message, channel);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(reintentoHandler, never()).reintentarOEnviarADlq(same(message), same(channel),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void delegaEnLaPoliticaDeReintentoAnteUnErrorRecuperable() throws Exception {
        DataAccessResourceFailureException bdCaida = new DataAccessResourceFailureException("BD no disponible");
        when(reader.leer(message)).thenReturn(evento);
        when(notificacionService.registrar(evento)).thenThrow(bdCaida);

        consumer.onCatalogoEvent(message, channel);

        verify(reintentoHandler).reintentarOEnviarADlq(message, channel, bdCaida);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
