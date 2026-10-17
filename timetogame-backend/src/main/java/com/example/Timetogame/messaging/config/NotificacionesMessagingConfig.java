package com.example.Timetogame.messaging.config;

import static com.example.Timetogame.messaging.config.NotificacionesTopologia.DLQ_QUEUE;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.DLQ_ROUTING_KEY;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.DLX_EXCHANGE;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.EVENTS_EXCHANGE;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.NOTIFICACIONES_BINDING_KEY;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.NOTIFICACIONES_QUEUE;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.RETRY_EXCHANGE;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.RETRY_QUEUE;
import static com.example.Timetogame.messaging.config.NotificacionesTopologia.RETRY_ROUTING_KEY;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declaracion de exchanges, colas y bindings del caso de uso "notificaciones
 * de catalogo". RabbitAdmin (autoconfigurado por Spring Boot) los declara en
 * el broker al conectar. Ver {@link NotificacionesTopologia} para el diagrama.
 */
@Configuration
public class NotificacionesMessagingConfig {

    // ---- Ruta principal: eventos de catalogo -> cola de notificaciones ----

    @Bean
    public TopicExchange catalogoEventsExchange() {
        return ExchangeBuilder.topicExchange(EVENTS_EXCHANGE).durable(true).build();
    }

    /**
     * Al recibir un basicNack sin requeue (o al vencer el reintento maximo), el
     * broker reenvia el mensaje al DLX con la routing key de la DLQ.
     */
    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(NOTIFICACIONES_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue())
                .to(catalogoEventsExchange())
                .with(NOTIFICACIONES_BINDING_KEY);
    }

    // ---- Reintento diferido: cola de espera con TTL que devuelve a la principal ----

    @Bean
    public DirectExchange catalogoRetryExchange() {
        return ExchangeBuilder.directExchange(RETRY_EXCHANGE).durable(true).build();
    }

    /**
     * Sin consumidores: el mensaje espera el TTL y el broker lo dead-letterea al
     * exchange por defecto ("") con la cola principal como routing key, o sea
     * vuelve solo a notificaciones (no a otras colas enlazadas al topic).
     */
    @Bean
    public Queue notificacionesRetryQueue(NotificacionesProperties properties) {
        return QueueBuilder.durable(RETRY_QUEUE)
                .ttl((int) properties.retrasoReintento().toMillis())
                .deadLetterExchange("")
                .deadLetterRoutingKey(NOTIFICACIONES_QUEUE)
                .build();
    }

    @Bean
    public Binding notificacionesRetryBinding(Queue notificacionesRetryQueue) {
        return BindingBuilder.bind(notificacionesRetryQueue)
                .to(catalogoRetryExchange())
                .with(RETRY_ROUTING_KEY);
    }

    // ---- Dead letter: mensajes que no se pudieron procesar ----

    @Bean
    public DirectExchange catalogoDeadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notificacionesDeadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding notificacionesDeadLetterBinding() {
        return BindingBuilder.bind(notificacionesDeadLetterQueue())
                .to(catalogoDeadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }
}
