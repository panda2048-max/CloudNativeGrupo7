package com.example.Timetogame.messaging.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Politica de reintentos de cada caso de uso (app.messaging.&lt;caso&gt;.*). */
@Validated
@ConfigurationProperties("app.messaging")
public record MensajeriaProperties(
        @DefaultValue @Valid Reintentos notificaciones,
        @DefaultValue @Valid Reintentos documentos
) {

    public record Reintentos(
            @DefaultValue("3") @Min(0) @Max(10) int maxReintentos,
            @DefaultValue("5s") @NotNull Duration retrasoReintento
    ) {
    }

    public PoliticaReintento politicaNotificaciones() {
        return new PoliticaReintento(TopologiaMensajeria.Notificaciones.RETRY_EXCHANGE,
                TopologiaMensajeria.Notificaciones.RETRY_ROUTING_KEY, notificaciones.maxReintentos(),
                notificaciones.retrasoReintento());
    }

    public PoliticaReintento politicaDocumentos() {
        return new PoliticaReintento(TopologiaMensajeria.Documentos.RETRY_EXCHANGE,
                TopologiaMensajeria.Documentos.RETRY_ROUTING_KEY, documentos.maxReintentos(),
                documentos.retrasoReintento());
    }
}
