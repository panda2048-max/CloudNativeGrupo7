package com.example.Timetogame.streaming.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.ExponentialBackOff;

import com.example.Timetogame.streaming.exception.EventoInvalidoException;

import lombok.extern.slf4j.Slf4j;

/**
 * Infraestructura comun de Kafka. Productores, consumidores y serializacion
 * se configuran en application.properties (spring.kafka.*); aqui va lo que
 * no se puede expresar como propiedad: la politica de errores de los
 * consumidores.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(StreamingProperties.class)
public class KafkaConfig {

    /**
     * Error en un consumidor:
     * <ul>
     *   <li>Recuperable (BD caida, timeout): 3 reintentos con backoff
     *       exponencial 1 s, 2 s, 4 s; si siguen fallando, al DLT.</li>
     *   <li>No recuperable (evento invalido o JSON que no deserializa): directo
     *       al DLT, sin reintentar.</li>
     * </ul>
     * En ambos casos se confirma el offset y el consumidor sigue con el
     * siguiente mensaje: un mensaje malo no detiene la particion.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxAttempts(3);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(EventoInvalidoException.class);
        handler.setRetryListeners((record, ex, intento) -> log.warn(
                "Fallo en el intento {} de {}-{}@{} (key={}): {}", intento, record.topic(), record.partition(),
                record.offset(), record.key(), causaRaiz(ex)));
        return handler;
    }

    /** "Listener failed" no dice nada: se muestra la excepcion original. */
    private static String causaRaiz(Throwable ex) {
        Throwable causa = ex;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getClass().getSimpleName() + ": " + causa.getMessage();
    }

    /**
     * Publica el mensaje fallido en &lt;topico&gt;.DLT, en la misma particion, con
     * headers que explican la causa (kafka_dlt-exception-message, -original-offset...).
     * Usa su propio template: un mensaje que no se pudo deserializar llega como
     * bytes crudos y se reenvia tal cual; uno valido se reserializa como JSON.
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new LinkedHashMap<>(kafkaProperties.buildProducerProperties());
        Map<Class<?>, org.apache.kafka.common.serialization.Serializer<?>> porTipo = new LinkedHashMap<>();
        porTipo.put(byte[].class, new ByteArraySerializer());
        // Sin setters: toma spring.json.add.type.headers=false de las propiedades del
        // productor (configurarlo por ambos lados lanza IllegalStateException).
        porTipo.put(Object.class, new JacksonJsonSerializer<>());
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config,
                new StringSerializer(), new DelegatingByTypeSerializer(porTipo, true));
        // Destino explicito: el sufijo por defecto cambio entre versiones de
        // Spring Kafka (".DLT" -> "-dlt") y el DLT debe ser el topico declarado en
        // KafkaTopicConfig (la creacion automatica de topicos esta desactivada).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(new KafkaTemplate<>(factory),
                (record, ex) -> new TopicPartition(record.topic() + TopologiaKafka.SUFIJO_DLT, record.partition()));
        recoverer.setLogRecoveryRecord(true);
        return recoverer;
    }
}
