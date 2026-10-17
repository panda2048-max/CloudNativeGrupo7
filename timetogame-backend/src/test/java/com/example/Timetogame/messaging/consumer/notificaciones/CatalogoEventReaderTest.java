package com.example.Timetogame.messaging.consumer.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.example.Timetogame.event.CatalogoEvent;
import com.example.Timetogame.event.EntidadCatalogo;
import com.example.Timetogame.event.TipoEventoCatalogo;
import com.example.Timetogame.messaging.exception.MensajeNoProcesableException;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import tools.jackson.databind.json.JsonMapper;

class CatalogoEventReaderTest {

    private static ValidatorFactory validatorFactory;
    private static CatalogoEventReader reader;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        reader = new CatalogoEventReader(JsonMapper.builder().build(), validatorFactory.getValidator());
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void leeUnEventoValido() {
        CatalogoEvent evento = reader.leer(mensaje("""
                {"eventId":"e-1","tipo":"CREADO","entidad":"VIDEOJUEGO","entidadId":5,
                 "nombre":"Halo","usuario":"admin@test","ocurridoEn":"2026-10-15T10:00:00Z"}
                """));

        assertThat(evento.eventId()).isEqualTo("e-1");
        assertThat(evento.tipo()).isEqualTo(TipoEventoCatalogo.CREADO);
        assertThat(evento.entidad()).isEqualTo(EntidadCatalogo.VIDEOJUEGO);
        assertThat(evento.entidadId()).isEqualTo(5L);
    }

    @Test
    void rechazaJsonMalformado() {
        assertThatThrownBy(() -> reader.leer(mensaje("{esto no es json")))
                .isInstanceOf(MensajeNoProcesableException.class)
                .hasMessageStartingWith("Payload JSON invalido");
    }

    @Test
    void rechazaValoresDeEnumDesconocidos() {
        assertThatThrownBy(() -> reader.leer(mensaje("""
                {"eventId":"e-1","tipo":"BORRADO","entidad":"VIDEOJUEGO","entidadId":5,
                 "nombre":"Halo","ocurridoEn":"2026-10-15T10:00:00Z"}
                """)))
                .isInstanceOf(MensajeNoProcesableException.class);
    }

    @Test
    void rechazaEventosConCamposObligatoriosFaltantes() {
        assertThatThrownBy(() -> reader.leer(mensaje("""
                {"eventId":"e-1","tipo":"CREADO","entidad":"VIDEOJUEGO","entidadId":-3}
                """)))
                .isInstanceOf(MensajeNoProcesableException.class)
                .hasMessageContaining("entidadId")
                .hasMessageContaining("nombre")
                .hasMessageContaining("ocurridoEn");
    }

    @Test
    void rechazaMensajeSinContenido() {
        assertThatThrownBy(() -> reader.leer(mensaje("null")))
                .isInstanceOf(MensajeNoProcesableException.class);
    }

    private static Message mensaje(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
