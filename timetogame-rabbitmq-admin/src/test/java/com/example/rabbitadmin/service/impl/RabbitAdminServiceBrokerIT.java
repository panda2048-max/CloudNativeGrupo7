package com.example.rabbitadmin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;

import com.example.rabbitadmin.client.RabbitManagementClient;
import com.example.rabbitadmin.config.RabbitAdminProperties;
import com.example.rabbitadmin.dto.BindingResponse;
import com.example.rabbitadmin.dto.ColaResponse;
import com.example.rabbitadmin.dto.CrearBindingRequest;
import com.example.rabbitadmin.dto.CrearColaRequest;
import com.example.rabbitadmin.dto.CrearExchangeRequest;
import com.example.rabbitadmin.dto.ExchangeResponse;
import com.example.rabbitadmin.dto.TipoCola;
import com.example.rabbitadmin.dto.TipoExchange;
import com.example.rabbitadmin.event.OperacionTopologiaEvent;
import com.example.rabbitadmin.exception.ConfiguracionInvalidaException;
import com.example.rabbitadmin.exception.ConflictoTopologiaException;
import com.example.rabbitadmin.exception.RecursoNoEncontradoException;
import com.example.rabbitadmin.security.UsuarioActualProvider;

/**
 * Prueba el servicio contra un RabbitMQ real (localhost, guest/guest, con el
 * plugin de management). No corre en un build normal:
 * <pre>./mvnw test -Dtest=RabbitAdminServiceBrokerIT -Drabbit.it=true</pre>
 */
@EnabledIfSystemProperty(named = "rabbit.it", matches = "true")
class RabbitAdminServiceBrokerIT {

    private static CachingConnectionFactory connectionFactory;
    private static RabbitAdminServiceImpl service;
    private static final List<OperacionTopologiaEvent> eventos = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");

        RabbitProperties rabbitProperties = new RabbitProperties();
        RabbitAdminProperties adminProperties = new RabbitAdminProperties(List.of("http://localhost:15672"),
                List.of("timetogame.catalogo.", "timetogame.documentos.", "timetogame.admin.", "amq."));

        service = new RabbitAdminServiceImpl(new RabbitAdmin(connectionFactory), new RabbitTemplate(connectionFactory),
                new RabbitManagementClient(adminProperties, rabbitProperties), adminProperties,
                evento -> eventos.add((OperacionTopologiaEvent) evento), new UsuarioActualProvider());
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void cicloCompletoContraElBroker() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String exchange = "it.pedidos.events." + sufijo;
        String exchangeDirect = "it.pedidos.direct." + sufijo;
        String cola = "it.pedidos." + sufijo;

        ExchangeResponse creado = service.crearExchange(new CrearExchangeRequest(exchange, TipoExchange.TOPIC, true, false));
        assertThat(creado.tipo()).isEqualTo("topic");
        service.crearExchange(new CrearExchangeRequest(exchangeDirect, TipoExchange.DIRECT, true, false));

        ColaResponse colaCreada = service.crearCola(new CrearColaRequest(cola, true, false, TipoCola.CLASSIC,
                null, null, 60_000, 100));
        assertThat(colaCreada.argumentos()).containsEntry("x-message-ttl", 60_000);
        assertThat(service.obtenerCola(cola).nombre()).isEqualTo(cola);
        assertThat(service.listarColas()).extracting(ColaResponse::nombre).contains(cola);

        service.crearBinding(new CrearBindingRequest(exchange, cola, "pedido.#"));
        assertThat(service.listarBindings()).contains(new BindingResponse(exchange, cola, "pedido.#"));

        // Duplicados -> 409
        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest(cola, null, null, null, null, null, null, null)))
                .isInstanceOf(ConflictoTopologiaException.class);
        assertThatThrownBy(() -> service.crearBinding(new CrearBindingRequest(exchange, cola, "pedido.#")))
                .isInstanceOf(ConflictoTopologiaException.class);

        // Configuraciones invalidas contra el estado real -> 400
        assertThatThrownBy(() -> service.crearBinding(new CrearBindingRequest(exchangeDirect, cola, "pedido.*")))
                .isInstanceOf(ConfiguracionInvalidaException.class);
        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest("it.otra." + sufijo, null, null, null,
                "it.no.existe." + sufijo, null, null, null)))
                .isInstanceOf(ConfiguracionInvalidaException.class);

        // Recursos del sistema -> protegidos
        assertThatThrownBy(() -> service.eliminarCola("timetogame.catalogo.notificaciones", false, false))
                .isInstanceOf(ConflictoTopologiaException.class)
                .hasMessageContaining("protegido");

        // Condicion del broker: no se puede borrar un exchange que aun tiene bindings
        assertThatThrownBy(() -> service.eliminarExchange(exchange, true))
                .isInstanceOf(ConflictoTopologiaException.class);

        // Limpieza en orden
        service.eliminarBinding(exchange, cola, "pedido.#");
        assertThatThrownBy(() -> service.eliminarBinding(exchange, cola, "pedido.#"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        service.eliminarCola(cola, true, true);
        service.eliminarExchange(exchange, true);
        service.eliminarExchange(exchangeDirect, false);

        assertThatThrownBy(() -> service.obtenerCola(cola)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.obtenerExchange(exchange)).isInstanceOf(RecursoNoEncontradoException.class);

        // Cada escritura exitosa genero su evento de auditoria (2 exchanges + cola + binding, y sus bajas)
        assertThat(eventos).hasSize(8);
    }
}
