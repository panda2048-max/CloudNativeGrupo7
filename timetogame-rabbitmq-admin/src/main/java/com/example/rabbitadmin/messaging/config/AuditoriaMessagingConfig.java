package com.example.rabbitadmin.messaging.config;

import static com.example.rabbitadmin.messaging.config.AuditoriaTopologia.AUDITORIA_BINDING_KEY;
import static com.example.rabbitadmin.messaging.config.AuditoriaTopologia.AUDITORIA_DLQ;
import static com.example.rabbitadmin.messaging.config.AuditoriaTopologia.AUDITORIA_DLQ_ROUTING_KEY;
import static com.example.rabbitadmin.messaging.config.AuditoriaTopologia.AUDITORIA_DLX;
import static com.example.rabbitadmin.messaging.config.AuditoriaTopologia.AUDITORIA_EXCHANGE;
import static com.example.rabbitadmin.messaging.config.AuditoriaTopologia.AUDITORIA_QUEUE;

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
 * Exchanges, colas y bindings del caso de uso "auditoria de la
 * administracion". Ver {@link AuditoriaTopologia} para el diagrama.
 */
@Configuration
public class AuditoriaMessagingConfig {

    // ---- Ruta principal ----

    @Bean
    public TopicExchange auditoriaExchange() {
        return ExchangeBuilder.topicExchange(AUDITORIA_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue auditoriaQueue() {
        return QueueBuilder.durable(AUDITORIA_QUEUE)
                .deadLetterExchange(AUDITORIA_DLX)
                .deadLetterRoutingKey(AUDITORIA_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding auditoriaBinding() {
        return BindingBuilder.bind(auditoriaQueue()).to(auditoriaExchange()).with(AUDITORIA_BINDING_KEY);
    }

    // ---- Dead letter ----

    @Bean
    public DirectExchange auditoriaDeadLetterExchange() {
        return ExchangeBuilder.directExchange(AUDITORIA_DLX).durable(true).build();
    }

    @Bean
    public Queue auditoriaDeadLetterQueue() {
        return QueueBuilder.durable(AUDITORIA_DLQ).build();
    }

    @Bean
    public Binding auditoriaDeadLetterBinding() {
        return BindingBuilder.bind(auditoriaDeadLetterQueue())
                .to(auditoriaDeadLetterExchange())
                .with(AUDITORIA_DLQ_ROUTING_KEY);
    }
}
