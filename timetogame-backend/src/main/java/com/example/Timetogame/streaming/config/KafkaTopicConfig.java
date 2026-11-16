package com.example.Timetogame.streaming.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topicos declarados en codigo: KafkaAdmin (autoconfigurado) los crea al
 * arrancar si no existen. Cada uno con la politica que su flujo necesita.
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Flujo de eventos (lo que paso): se borra por tiempo. 7 dias alcanzan para
     * reprocesar o levantar un consumidor nuevo desde el inicio.
     */
    @Bean
    public NewTopic actividadTopic(StreamingProperties properties) {
        StreamingProperties.Topicos t = properties.topicos();
        return TopicBuilder.name(TopologiaKafka.ACTIVIDAD)
                .partitions(t.particiones())
                .replicas(t.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retencionActividad().toMillis()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncEfectivo()))
                .build();
    }

    /**
     * Estado del catalogo: compactado, Kafka conserva al menos el ultimo
     * evento de cada videojuego (key). Un consumidor nuevo reconstruye el
     * catalogo leyendo el topico sin depender de la base de datos.
     */
    @Bean
    public NewTopic catalogoCambiosTopic(StreamingProperties properties) {
        StreamingProperties.Topicos t = properties.topicos();
        return TopicBuilder.name(TopologiaKafka.CATALOGO_CAMBIOS)
                .partitions(t.particiones())
                .replicas(t.replicas())
                .compact()
                .config(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, "60000")
                .config(TopicConfig.SEGMENT_MS_CONFIG, "3600000")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncEfectivo()))
                .build();
    }

    /**
     * DLT: mismas particiones que el original (el recoverer publica en la
     * misma particion) y retencion mas larga para alcanzar a revisarlos.
     */
    @Bean
    public NewTopic actividadDltTopic(StreamingProperties properties) {
        return dlt(TopologiaKafka.ACTIVIDAD_DLT, properties.topicos());
    }

    @Bean
    public NewTopic catalogoCambiosDltTopic(StreamingProperties properties) {
        return dlt(TopologiaKafka.CATALOGO_CAMBIOS_DLT, properties.topicos());
    }

    private static NewTopic dlt(String nombre, StreamingProperties.Topicos t) {
        return TopicBuilder.name(nombre)
                .partitions(t.particiones())
                .replicas(t.replicas())
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retencionDlt().toMillis()))
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncEfectivo()))
                .build();
    }
}
