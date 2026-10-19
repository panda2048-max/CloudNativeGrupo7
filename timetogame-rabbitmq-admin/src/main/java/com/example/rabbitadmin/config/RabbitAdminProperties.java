package com.example.rabbitadmin.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("rabbit-admin")
public record RabbitAdminProperties(
        /**
         * URLs base de la API HTTP de management (plugin rabbitmq_management),
         * una por nodo del cluster. Se usa la primera que responda: todos los
         * nodos ven la misma topologia.
         */
        @NotEmpty List<String> managementUrls,
        /** Prefijos de nombres que la API se niega a eliminar. */
        @NotNull @DefaultValue List<String> recursosProtegidos
) {

    public boolean esProtegido(String nombre) {
        return recursosProtegidos.stream().anyMatch(nombre::startsWith);
    }
}
