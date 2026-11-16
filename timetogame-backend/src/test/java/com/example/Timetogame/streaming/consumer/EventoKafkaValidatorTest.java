package com.example.Timetogame.streaming.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.Timetogame.streaming.event.OrigenActividad;
import com.example.Timetogame.streaming.event.ActividadEvent;
import com.example.Timetogame.streaming.event.TipoActividad;
import com.example.Timetogame.streaming.exception.EventoInvalidoException;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class EventoKafkaValidatorTest {

    private static ValidatorFactory factory;
    private static EventoKafkaValidator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = new EventoKafkaValidator(factory.getValidator());
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void aceptaUnEventoCompleto() {
        ActividadEvent evento = new ActividadEvent("e-1", Instant.now(), TipoActividad.VISTA, 7L, "Halo", null,
                OrigenActividad.DETALLE, null, null);
        assertThat(validator.validar(record(evento))).isSameAs(evento);
    }

    @Test
    void rechazaCamposFaltantesIndicandoLaUbicacion() {
        ActividadEvent invalido = new ActividadEvent("", null, TipoActividad.VISTA, -1L, null, null, null, null, null);

        assertThatThrownBy(() -> validator.validar(record(invalido)))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("timetogame.actividad-2@41 (key=7)")
                .hasMessageContaining("videojuegoId")
                .hasMessageContaining("ocurridoEn");
    }

    @Test
    void rechazaUnMensajeSinValor() {
        assertThatThrownBy(() -> validator.validar(record(null))).isInstanceOf(EventoInvalidoException.class);
    }

    private static ConsumerRecord<String, ActividadEvent> record(ActividadEvent valor) {
        return new ConsumerRecord<>("timetogame.actividad", 2, 41L, "7", valor);
    }
}
