package com.example.Timetogame.streaming.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envio comun a los productores: KafkaTemplate con value JSON (JacksonJsonSerializer,
 * ver spring.kafka.producer.*) y key = id del videojuego. Con acks=all e
 * idempotencia, el broker confirma recien cuando la escritura esta en las
 * replicas sincronizadas. Un fallo se registra con contexto y no se propaga:
 * los productores corren en hilos @Async, fuera de la peticion del usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class KafkaEventoPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    void publicar(String topico, String key, String eventId, Object evento) {
        try {
            kafkaTemplate.send(topico, key, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar el evento {} en {} (key={}): {}", eventId, topico, key,
                            error.getMessage());
                } else {
                    log.info("Evento {} publicado en {}-{}@{} (key={})", eventId, topico,
                            resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset(), key);
                }
            });
        } catch (RuntimeException e) {
            // send() falla de inmediato si no hay metadata del cluster (Kafka caido,
            // tras max.block.ms) o si el productor no se pudo crear. Corre en un hilo
            // @Async: si no se registra aqui, el error se pierde sin contexto.
            log.error("Kafka no disponible: se pierde el evento {} para {} (key={})", eventId, topico, key, e);
        }
    }
}
