package com.example.Timetogame.messaging.consumer.documentos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.Timetogame.event.AccionFicha;
import com.example.Timetogame.event.SolicitudFicha;
import com.example.Timetogame.exception.ResourceNotFoundException;
import com.example.Timetogame.messaging.config.MensajeriaProperties;
import com.example.Timetogame.messaging.consumer.support.MensajeJsonReader;
import com.example.Timetogame.messaging.consumer.support.ReintentoDiferidoHandler;
import com.example.Timetogame.messaging.exception.MensajeNoProcesableException;
import com.example.Timetogame.service.FichaVideojuegoService;
import com.rabbitmq.client.Channel;

class FichaVideojuegoConsumerTest {

    private static final long DELIVERY_TAG = 11L;
    private static final MensajeriaProperties PROPERTIES = new MensajeriaProperties(
            new MensajeriaProperties.Reintentos(3, Duration.ofSeconds(5)),
            new MensajeriaProperties.Reintentos(2, Duration.ofSeconds(10)));

    private final MensajeJsonReader reader = mock(MensajeJsonReader.class);
    private final FichaVideojuegoService fichaService = mock(FichaVideojuegoService.class);
    private final ReintentoDiferidoHandler reintentoHandler = mock(ReintentoDiferidoHandler.class);
    private final Channel channel = mock(Channel.class);

    private FichaVideojuegoConsumer consumer;
    private Message message;

    @BeforeEach
    void setUp() {
        consumer = new FichaVideojuegoConsumer(reader, fichaService, reintentoHandler, PROPERTIES);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(DELIVERY_TAG);
        message = new Message("{}".getBytes(), props);
    }

    @Test
    void generaLaFichaYConfirma() throws Exception {
        SolicitudFicha solicitud = SolicitudFicha.de(AccionFicha.GENERAR, 7L, "admin@test");
        when(reader.leer(message, SolicitudFicha.class)).thenReturn(solicitud);
        when(fichaService.generar(solicitud)).thenReturn(true);

        consumer.onSolicitudFicha(message, channel);

        verify(fichaService).generar(solicitud);
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void eliminaLaFichaYConfirma() throws Exception {
        SolicitudFicha solicitud = SolicitudFicha.de(AccionFicha.ELIMINAR, 7L, "admin@test");
        when(reader.leer(message, SolicitudFicha.class)).thenReturn(solicitud);

        consumer.onSolicitudFicha(message, channel);

        verify(fichaService).eliminar(solicitud);
        verify(fichaService, never()).generar(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void enviaALaDlqSinReintentarSiElVideojuegoNoExiste() throws Exception {
        SolicitudFicha solicitud = SolicitudFicha.de(AccionFicha.GENERAR, 999L, "admin@test");
        when(reader.leer(message, SolicitudFicha.class)).thenReturn(solicitud);
        when(fichaService.generar(solicitud)).thenThrow(new ResourceNotFoundException("no existe"));

        consumer.onSolicitudFicha(message, channel);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(reintentoHandler, never()).reintentarOEnviarADlq(any(), any(), any(), any());
    }

    @Test
    void enviaALaDlqSinReintentarUnMensajeInvalido() throws Exception {
        when(reader.leer(message, SolicitudFicha.class)).thenThrow(new MensajeNoProcesableException("invalido"));

        consumer.onSolicitudFicha(message, channel);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void reintentaConLaPoliticaDeDocumentosAnteUnErrorRecuperable() throws Exception {
        SolicitudFicha solicitud = SolicitudFicha.de(AccionFicha.GENERAR, 7L, "admin@test");
        DataIntegrityViolationException conflicto = new DataIntegrityViolationException("duplicado");
        when(reader.leer(message, SolicitudFicha.class)).thenReturn(solicitud);
        when(fichaService.generar(solicitud)).thenThrow(conflicto);

        consumer.onSolicitudFicha(message, channel);

        verify(reintentoHandler).reintentarOEnviarADlq(message, channel, conflicto, PROPERTIES.politicaDocumentos());
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
