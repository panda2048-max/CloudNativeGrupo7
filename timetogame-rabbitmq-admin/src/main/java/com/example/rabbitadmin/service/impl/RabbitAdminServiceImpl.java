package com.example.rabbitadmin.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.example.rabbitadmin.client.RabbitManagementClient;
import com.example.rabbitadmin.client.RabbitManagementClient.BindingInfo;
import com.example.rabbitadmin.client.RabbitManagementClient.ColaInfo;
import com.example.rabbitadmin.client.RabbitManagementClient.ExchangeInfo;
import com.example.rabbitadmin.config.RabbitAdminProperties;
import com.example.rabbitadmin.dto.BindingResponse;
import com.example.rabbitadmin.dto.ColaResponse;
import com.example.rabbitadmin.dto.CrearBindingRequest;
import com.example.rabbitadmin.dto.CrearColaRequest;
import com.example.rabbitadmin.dto.CrearExchangeRequest;
import com.example.rabbitadmin.dto.ExchangeResponse;
import com.example.rabbitadmin.dto.TipoCola;
import com.example.rabbitadmin.event.OperacionTopologiaEvent;
import com.example.rabbitadmin.event.TipoOperacion;
import com.example.rabbitadmin.event.TipoRecurso;
import com.example.rabbitadmin.exception.BrokerNoDisponibleException;
import com.example.rabbitadmin.exception.ConfiguracionInvalidaException;
import com.example.rabbitadmin.exception.ConflictoTopologiaException;
import com.example.rabbitadmin.exception.RecursoNoEncontradoException;
import com.example.rabbitadmin.security.UsuarioActualProvider;
import com.example.rabbitadmin.service.RabbitAdminService;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;

import lombok.RequiredArgsConstructor;

/**
 * Escrituras via AmqpAdmin (protocolo AMQP) y lecturas via la API HTTP de
 * management, que es la unica que permite listar. Antes de cada escritura se
 * valida contra el estado real del broker para devolver errores claros (404,
 * 409, 400) en vez de los cierres de canal crudos de AMQP; si igual ocurre uno
 * (carrera con otro cliente), se traduce en {@link #traducir}.
 */
@Service
@RequiredArgsConstructor
public class RabbitAdminServiceImpl implements RabbitAdminService {

    private static final String TIPO_DIRECT = "direct";

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitManagementClient managementClient;
    private final RabbitAdminProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final UsuarioActualProvider usuarioActualProvider;

    // ------------------------------------------------------------------ colas

    @Override
    public List<ColaResponse> listarColas() {
        return managementClient.listarColas().stream()
                .map(RabbitAdminServiceImpl::toResponse)
                .sorted(Comparator.comparing(ColaResponse::nombre))
                .toList();
    }

    @Override
    public ColaResponse obtenerCola(String nombre) {
        return managementClient.buscarCola(nombre)
                .map(RabbitAdminServiceImpl::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la cola '" + nombre + "'"));
    }

    @Override
    public ColaResponse crearCola(CrearColaRequest request) {
        if (managementClient.buscarCola(request.nombre()).isPresent()) {
            throw new ConflictoTopologiaException("Ya existe una cola llamada '" + request.nombre() + "'");
        }
        String dlx = request.deadLetterExchange();
        if (dlx != null && managementClient.buscarExchange(dlx).isEmpty()) {
            throw new ConfiguracionInvalidaException("El dead letter exchange '" + dlx + "' no existe: "
                    + "crealo antes, o el broker descartaria sin aviso los mensajes rechazados");
        }

        Queue cola = construirCola(request);
        ejecutar("Crear la cola", () -> amqpAdmin.declareQueue(cola));
        auditar(TipoOperacion.CREAR, TipoRecurso.COLA, describir(request));

        return new ColaResponse(cola.getName(), request.tipoEfectivo().name().toLowerCase(Locale.ROOT),
                cola.isDurable(), cola.isAutoDelete(), cola.getArguments(), 0L, 0);
    }

    @Override
    public void eliminarCola(String nombre, boolean soloSiSinConsumidores, boolean soloSiVacia) {
        verificarNoProtegido(nombre);
        obtenerCola(nombre);

        ejecutar("Eliminar la cola", () -> amqpAdmin.deleteQueue(nombre, soloSiSinConsumidores, soloSiVacia));
        auditar(TipoOperacion.ELIMINAR, TipoRecurso.COLA, "cola '" + nombre + "'");
    }

    // -------------------------------------------------------------- exchanges

    @Override
    public List<ExchangeResponse> listarExchanges() {
        return managementClient.listarExchanges().stream()
                .filter(exchange -> !exchange.name().isEmpty()) // el exchange por defecto no es administrable
                .map(RabbitAdminServiceImpl::toResponse)
                .sorted(Comparator.comparing(ExchangeResponse::nombre))
                .toList();
    }

    @Override
    public ExchangeResponse obtenerExchange(String nombre) {
        return toResponse(buscarExchange(nombre));
    }

    @Override
    public ExchangeResponse crearExchange(CrearExchangeRequest request) {
        if (managementClient.buscarExchange(request.nombre()).isPresent()) {
            throw new ConflictoTopologiaException("Ya existe un exchange llamado '" + request.nombre() + "'");
        }

        Exchange exchange = switch (request.tipo()) {
            case DIRECT -> new DirectExchange(request.nombre(), request.esDurable(), request.esAutoDelete());
            case TOPIC -> new TopicExchange(request.nombre(), request.esDurable(), request.esAutoDelete());
            case FANOUT -> new FanoutExchange(request.nombre(), request.esDurable(), request.esAutoDelete());
            case HEADERS -> new HeadersExchange(request.nombre(), request.esDurable(), request.esAutoDelete());
        };
        ejecutar("Crear el exchange", () -> amqpAdmin.declareExchange(exchange));
        auditar(TipoOperacion.CREAR, TipoRecurso.EXCHANGE, "exchange '%s' (%s, %s)".formatted(exchange.getName(),
                exchange.getType(), exchange.isDurable() ? "durable" : "transitorio"));

        return new ExchangeResponse(exchange.getName(), exchange.getType(), exchange.isDurable(),
                exchange.isAutoDelete());
    }

    @Override
    public void eliminarExchange(String nombre, boolean soloSiSinBindings) {
        verificarNoProtegido(nombre);
        buscarExchange(nombre);

        // AmqpAdmin no expone la condicion "if-unused" para exchanges; se usa el canal directamente.
        ejecutar("Eliminar el exchange", () -> rabbitTemplate.execute(channel -> {
            channel.exchangeDelete(nombre, soloSiSinBindings);
            return null;
        }));
        auditar(TipoOperacion.ELIMINAR, TipoRecurso.EXCHANGE, "exchange '" + nombre + "'");
    }

    // --------------------------------------------------------------- bindings

    @Override
    public List<BindingResponse> listarBindings() {
        return managementClient.listarBindings().stream()
                // Los bindings implicitos del exchange por defecto no se administran.
                .filter(binding -> !binding.source().isEmpty() && "queue".equals(binding.destinationType()))
                .map(binding -> new BindingResponse(binding.source(), binding.destination(), binding.routingKey()))
                .sorted(Comparator.comparing(BindingResponse::exchange).thenComparing(BindingResponse::cola))
                .toList();
    }

    @Override
    public BindingResponse crearBinding(CrearBindingRequest request) {
        ExchangeInfo exchange = buscarExchange(request.exchange());
        obtenerCola(request.cola());
        String routingKey = request.routingKeyEfectiva();

        if (TIPO_DIRECT.equals(exchange.type()) && (routingKey.contains("*") || routingKey.contains("#"))) {
            throw new ConfiguracionInvalidaException("Los comodines * y # solo tienen efecto en exchanges topic; "
                    + "el exchange '" + exchange.name() + "' es direct y los compararia literalmente");
        }
        if (existeBinding(request.exchange(), request.cola(), routingKey)) {
            throw new ConflictoTopologiaException("Ya existe el binding %s -> %s con routing key '%s'"
                    .formatted(request.exchange(), request.cola(), routingKey));
        }

        ejecutar("Crear el binding", () -> amqpAdmin.declareBinding(binding(request.exchange(), request.cola(),
                routingKey)));
        auditar(TipoOperacion.CREAR, TipoRecurso.BINDING, "binding %s -> %s (routing key '%s')"
                .formatted(request.exchange(), request.cola(), routingKey));

        return new BindingResponse(request.exchange(), request.cola(), routingKey);
    }

    @Override
    public void eliminarBinding(String exchange, String cola, String routingKey) {
        verificarNoProtegido(exchange);
        verificarNoProtegido(cola);
        if (!existeBinding(exchange, cola, routingKey)) {
            throw new RecursoNoEncontradoException("No existe el binding %s -> %s con routing key '%s'"
                    .formatted(exchange, cola, routingKey));
        }

        ejecutar("Eliminar el binding", () -> amqpAdmin.removeBinding(binding(exchange, cola, routingKey)));
        auditar(TipoOperacion.ELIMINAR, TipoRecurso.BINDING, "binding %s -> %s (routing key '%s')"
                .formatted(exchange, cola, routingKey));
    }

    // ---------------------------------------------------------------- soporte

    private static Queue construirCola(CrearColaRequest request) {
        QueueBuilder builder = request.esDurable()
                ? QueueBuilder.durable(request.nombre())
                : QueueBuilder.nonDurable(request.nombre());
        if (request.esAutoDelete()) {
            builder.autoDelete();
        }
        if (request.tipoEfectivo() == TipoCola.QUORUM) {
            builder.quorum();
        }
        if (request.deadLetterExchange() != null) {
            builder.deadLetterExchange(request.deadLetterExchange());
        }
        if (request.deadLetterRoutingKey() != null) {
            builder.deadLetterRoutingKey(request.deadLetterRoutingKey());
        }
        if (request.ttlMensajesMs() != null) {
            builder.ttl(request.ttlMensajesMs());
        }
        if (request.maxMensajes() != null) {
            builder.maxLength(request.maxMensajes());
        }
        return builder.build();
    }

    private static String describir(CrearColaRequest request) {
        List<String> detalles = new ArrayList<>();
        detalles.add(request.tipoEfectivo().name().toLowerCase(Locale.ROOT));
        detalles.add(request.esDurable() ? "durable" : "transitoria");
        if (request.esAutoDelete()) {
            detalles.add("auto-delete");
        }
        if (request.deadLetterExchange() != null) {
            detalles.add("dlx=" + request.deadLetterExchange());
        }
        if (request.ttlMensajesMs() != null) {
            detalles.add("ttl=" + request.ttlMensajesMs() + "ms");
        }
        if (request.maxMensajes() != null) {
            detalles.add("max=" + request.maxMensajes());
        }
        return "cola '%s' (%s)".formatted(request.nombre(), String.join(", ", detalles));
    }

    private ExchangeInfo buscarExchange(String nombre) {
        return managementClient.buscarExchange(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el exchange '" + nombre + "'"));
    }

    private boolean existeBinding(String exchange, String cola, String routingKey) {
        return managementClient.bindingsEntre(exchange, cola).stream()
                .map(BindingInfo::routingKey)
                .anyMatch(routingKey::equals);
    }

    private static Binding binding(String exchange, String cola, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private void verificarNoProtegido(String nombre) {
        if (properties.esProtegido(nombre)) {
            throw new ConflictoTopologiaException("'" + nombre + "' es un recurso protegido (topologia del sistema "
                    + "o reservado por RabbitMQ) y no se puede modificar desde esta API");
        }
    }

    private void auditar(TipoOperacion operacion, TipoRecurso recurso, String descripcion) {
        eventPublisher.publishEvent(OperacionTopologiaEvent.de(operacion, recurso, descripcion,
                usuarioActualProvider.usuarioActual()));
    }

    private static void ejecutar(String operacion, Runnable accion) {
        try {
            accion.run();
        } catch (AmqpException e) {
            throw traducir(operacion, e);
        }
    }

    /** Traduce el cierre de canal AMQP (reply code del broker) a un error HTTP con sentido. */
    static RuntimeException traducir(String operacion, AmqpException e) {
        if (e instanceof AmqpConnectException) {
            return new BrokerNoDisponibleException("No hay conexion con RabbitMQ", e);
        }
        Throwable causa = e;
        while (causa != null && !(causa instanceof ShutdownSignalException)) {
            causa = causa.getCause();
        }
        if (causa instanceof ShutdownSignalException senal && senal.getReason() instanceof AMQP.Channel.Close cierre) {
            String detalle = operacion + ": " + cierre.getReplyText();
            return switch (cierre.getReplyCode()) {
                case AMQP.NOT_FOUND -> new RecursoNoEncontradoException(detalle);
                case AMQP.PRECONDITION_FAILED, AMQP.ACCESS_REFUSED, AMQP.RESOURCE_LOCKED ->
                        new ConflictoTopologiaException(detalle);
                default -> e;
            };
        }
        return e;
    }

    private static ColaResponse toResponse(ColaInfo info) {
        Map<String, Object> argumentos = info.arguments() == null ? Map.of() : info.arguments();
        return new ColaResponse(info.name(), info.type(), info.durable(), info.autoDelete(), argumentos,
                info.messages(), info.consumers());
    }

    private static ExchangeResponse toResponse(ExchangeInfo info) {
        return new ExchangeResponse(info.name(), info.type(), info.durable(), info.autoDelete());
    }
}
