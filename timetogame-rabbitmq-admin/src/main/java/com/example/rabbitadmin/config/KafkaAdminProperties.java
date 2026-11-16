package com.example.rabbitadmin.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("kafka-admin")
public record KafkaAdminProperties(
        /** Prefijos de topicos que la API se niega a eliminar (los del sistema e internos de Kafka). */
        @NotNull @DefaultValue({"timetogame.", "__"}) List<String> topicosProtegidos,
        /** Tiempo maximo de espera de cada operacion contra el cluster. */
        @NotNull @DefaultValue("10s") Duration timeout
) {

    public boolean esProtegido(String topico) {
        return topicosProtegidos.stream().anyMatch(topico::startsWith);
    }
}
