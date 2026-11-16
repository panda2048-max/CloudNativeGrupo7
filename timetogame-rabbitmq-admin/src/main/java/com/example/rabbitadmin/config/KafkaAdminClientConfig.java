package com.example.rabbitadmin.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AdminClient de Kafka construido con spring.kafka.* (bootstrap-servers y
 * spring.kafka.admin.properties). Es thread-safe y se reutiliza en todas las
 * peticiones; Spring lo cierra al apagar.
 */
@Configuration
public class KafkaAdminClientConfig {

    @Bean(destroyMethod = "close")
    public AdminClient kafkaAdminClient(KafkaProperties kafkaProperties) {
        return AdminClient.create(kafkaProperties.buildAdminProperties());
    }
}
