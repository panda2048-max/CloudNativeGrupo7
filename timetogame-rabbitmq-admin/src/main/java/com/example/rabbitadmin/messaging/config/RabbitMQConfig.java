package com.example.rabbitadmin.messaging.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Infraestructura comun de RabbitMQ: serializacion JSON para publicar,
 * confirmaciones del broker hacia el productor y la factory de listeners con
 * ACK manual. Misma configuracion que timetogame-backend.
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String MANUAL_ACK_CONTAINER_FACTORY = "manualAckContainerFactory";

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplateCustomizer confirmacionesDePublicacion() {
        return template -> {
            template.setConfirmCallback((correlation, ack, cause) -> {
                String id = correlation != null ? correlation.getId() : "sin-correlacion";
                if (ack) {
                    log.debug("Broker confirmo el mensaje {}", id);
                } else {
                    log.error("Broker rechazo (nack) el mensaje {}: {}", id, cause);
                }
            });
            template.setReturnsCallback(returned -> log.error(
                    "Mensaje no entregado: ninguna cola enlazada. exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
                    returned.getReplyText()));
        };
    }

    @Bean(MANUAL_ACK_CONTAINER_FACTORY)
    public SimpleRabbitListenerContainerFactory manualAckContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(new CuerpoCrudoMessageConverter());
        factory.setPrefetchCount(10);
        return factory;
    }

    /**
     * Entrega el cuerpo sin interpretarlo; cada consumidor deserializa dentro de
     * su try/catch para decidir ack/nack el mismo. Si el contenedor convirtiera
     * antes, un payload invalido fallaria fuera del listener y en la DLQ el
     * ErrorHandler por defecto lo descartaria en silencio.
     */
    static final class CuerpoCrudoMessageConverter implements MessageConverter {

        @Override
        public Message toMessage(Object object, MessageProperties messageProperties) {
            throw new MessageConversionException("Converter exclusivo de consumidores: no se usa para publicar");
        }

        @Override
        public Object fromMessage(Message message) {
            return message.getBody();
        }
    }
}
