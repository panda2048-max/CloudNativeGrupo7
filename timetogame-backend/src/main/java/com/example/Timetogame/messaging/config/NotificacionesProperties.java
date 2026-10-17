package com.example.Timetogame.messaging.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("app.messaging.notificaciones")
public record NotificacionesProperties(
        @DefaultValue("3") @Min(0) @Max(10) int maxReintentos,
        @DefaultValue("5s") @NotNull Duration retrasoReintento
) {
}
