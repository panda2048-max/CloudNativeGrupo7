package com.example.Timetogame.messaging.consumer.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.example.Timetogame.event.AccionFicha;
import com.example.Timetogame.event.CatalogoEvent;
import com.example.Timetogame.event.EntidadCatalogo;
import com.example.Timetogame.event.SolicitudFicha;
import com.example.Timetogame.event.TipoEventoCatalogo;
import com.example.Timetogame.messaging.exception.MensajeNoProcesableException;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import tools.jackson.databind.json.JsonMapper;

class MensajeJsonReaderTest {

    private static ValidatorFactory validatorFactory;
    private static MensajeJsonReader reader;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        reader = new MensajeJsonReader(JsonMapper.builder().build(), validatorFactory.getValidator());
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void leeUnEventoDeCatalogoValido() {
        CatalogoEvent evento = reader.leer(mensaje("""
                {"eventId":"e-1","tipo":"CREADO","entidad":"VIDEOJUEGO","entidadId":5,
                 "nombre":"Halo","usuario":"admin@test","ocurridoEn":"2026-10-15T10:00:00Z"}
                """), CatalogoEvent.class);

        assertThat(evento.eventId()).isEqualTo("e-1");
        assertThat(evento.tipo()).isEqualTo(TipoEventoCatalogo.CREADO);
        assertThat(evento.entidad()).isEqualTo(EntidadCatalogo.VIDEOJUEGO);
        assertThat(evento.entidadId()).isEqualTo(5L);
    }

    @Test
    void leeUnaSolicitudDeFichaValida() {
        SolicitudFicha solicitud = reader.leer(mensaje("""
                {"solicitudId":"s-1","accion":"GENERAR","videojuegoId":7,
                 "solicitadoPor":"admin@test","solicitadoEn":"2026-10-15T10:00:00Z"}
                """), SolicitudFicha.class);

        assertThat(solicitud.accion()).isEqualTo(AccionFicha.GENERAR);
        assertThat(solicitud.videojuegoId()).isEqualTo(7L);
    }

    @Test
    void rechazaJsonMalformado() {
        assertThatThrownBy(() -> reader.leer(mensaje("{esto no es json"), CatalogoEvent.class))
                .isInstanceOf(MensajeNoProcesableException.class)
                .hasMessageStartingWith("Payload JSON invalido");
    }

    @Test
    void rechazaValoresDeEnumDesconocidos() {
        assertThatThrownBy(() -> reader.leer(mensaje("""
                {"eventId":"e-1","tipo":"BORRADO","entidad":"VIDEOJUEGO","entidadId":5,
                 "nombre":"Halo","ocurridoEn":"2026-10-15T10:00:00Z"}
                """), CatalogoEvent.class))
                .isInstanceOf(MensajeNoProcesableException.class);
    }

    @Test
    void rechazaMensajesConCamposObligatoriosFaltantes() {
        assertThatThrownBy(() -> reader.leer(mensaje("""
                {"eventId":"e-1","tipo":"CREADO","entidad":"VIDEOJUEGO","entidadId":-3}
                """), CatalogoEvent.class))
                .isInstanceOf(MensajeNoProcesableException.class)
                .hasMessageContaining("entidadId")
                .hasMessageContaining("nombre")
                .hasMessageContaining("ocurridoEn");

        assertThatThrownBy(() -> reader.leer(mensaje("""
                {"solicitudId":"","accion":"GENERAR"}
                """), SolicitudFicha.class))
                .isInstanceOf(MensajeNoProcesableException.class)
                .hasMessageContaining("solicitudId")
                .hasMessageContaining("videojuegoId");
    }

    @Test
    void rechazaMensajeSinContenido() {
        assertThatThrownBy(() -> reader.leer(mensaje("null"), CatalogoEvent.class))
                .isInstanceOf(MensajeNoProcesableException.class);
    }

    private static Message mensaje(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
