package com.example.Timetogame.messaging.config;

import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.DLQ_QUEUE;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.DLQ_ROUTING_KEY;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.DLX_EXCHANGE;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.ELIMINAR_ROUTING_KEY;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.EXCHANGE;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.FICHAS_QUEUE;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.GENERAR_ROUTING_KEY;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.RETRY_EXCHANGE;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.RETRY_QUEUE;
import static com.example.Timetogame.messaging.config.TopologiaMensajeria.Documentos.RETRY_ROUTING_KEY;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declaracion de exchanges, colas y bindings del caso de uso "generacion de
 * fichas tecnicas". Exchange direct: cada accion (generar / eliminar) tiene
 * su routing key exacta y ambas se enlazan a la misma cola de trabajo.
 * Ver {@link TopologiaMensajeria} para el diagrama.
 */
@Configuration
public class DocumentosMessagingConfig {

    // ---- Ruta principal: solicitudes de ficha -> cola de trabajo ----

    @Bean
    public DirectExchange documentosExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue fichasQueue() {
        return QueueBuilder.durable(FICHAS_QUEUE)
                .quorum()
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding fichasGenerarBinding() {
        return BindingBuilder.bind(fichasQueue()).to(documentosExchange()).with(GENERAR_ROUTING_KEY);
    }

    @Bean
    public Binding fichasEliminarBinding() {
        return BindingBuilder.bind(fichasQueue()).to(documentosExchange()).with(ELIMINAR_ROUTING_KEY);
    }

    // ---- Reintento diferido (mismo esquema que notificaciones) ----

    @Bean
    public DirectExchange documentosRetryExchange() {
        return ExchangeBuilder.directExchange(RETRY_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue fichasRetryQueue(MensajeriaProperties properties) {
        return QueueBuilder.durable(RETRY_QUEUE)
                .quorum()
                .ttl((int) properties.documentos().retrasoReintento().toMillis())
                .deadLetterExchange("")
                .deadLetterRoutingKey(FICHAS_QUEUE)
                .build();
    }

    @Bean
    public Binding fichasRetryBinding(Queue fichasRetryQueue) {
        return BindingBuilder.bind(fichasRetryQueue).to(documentosRetryExchange()).with(RETRY_ROUTING_KEY);
    }

    // ---- Dead letter ----

    @Bean
    public DirectExchange documentosDeadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue fichasDeadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).quorum().build();
    }

    @Bean
    public Binding fichasDeadLetterBinding() {
        return BindingBuilder.bind(fichasDeadLetterQueue()).to(documentosDeadLetterExchange()).with(DLQ_ROUTING_KEY);
    }
}
