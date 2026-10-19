package com.example.Timetogame.messaging.consumer.support;

import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Headers.REINTENTOS;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Headers.ROUTING_KEY_ORIGINAL;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Headers.ULTIMO_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.example.Timetogame.dto.MensajeFallidoDTO;
import com.example.Timetogame.service.MensajeFallidoService;
import com.rabbitmq.client.Channel;

class RegistroDlqHandlerTest {

    private final MensajeFallidoService mensajeFallidoService = mock(MensajeFallidoService.class);
    private final Channel channel = mock(Channel.class);
    private final RegistroDlqHandler handler = new RegistroDlqHandler(mensajeFallidoService);

    @Test
    void registraElMensajeMuertoConSuDiagnosticoYConfirma() throws Exception {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(9L);
        props.setMessageId("e-9");
        props.setHeader(REINTENTOS, 3);
        props.setHeader(ULTIMO_ERROR, "DataAccessResourceFailureException: BD no disponible");
        props.setHeader(ROUTING_KEY_ORIGINAL, "catalogo.videojuego.eliminado");
        Message message = new Message("{\"eventId\":\"e-9\"}".getBytes(StandardCharsets.UTF_8), props);

        handler.registrar(message, channel);

        ArgumentCaptor<MensajeFallidoDTO> registro = ArgumentCaptor.forClass(MensajeFallidoDTO.class);
        verify(mensajeFallidoService).registrar(registro.capture());
        assertThat(registro.getValue().messageId()).isEqualTo("e-9");
        assertThat(registro.getValue().reintentos()).isEqualTo(3);
        assertThat(registro.getValue().routingKey()).isEqualTo("catalogo.videojuego.eliminado");
        assertThat(registro.getValue().ultimoError()).contains("BD no disponible");
        assertThat(registro.getValue().payload()).isEqualTo("{\"eventId\":\"e-9\"}");

        verify(channel).basicAck(9L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
