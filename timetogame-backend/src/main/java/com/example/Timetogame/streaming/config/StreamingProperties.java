package com.example.Timetogame.streaming.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Parametros de topicos y del calculo de tendencias (app.kafka.*). */
@Validated
@ConfigurationProperties("app.kafka")
public record StreamingProperties(
        @DefaultValue @Valid Topicos topicos,
        @DefaultValue @Valid Tendencias tendencias
) {

    /**
     * 3 particiones = hasta 3 consumidores del mismo grupo en paralelo;
     * 3 replicas con min.insync.replicas=2 = una escritura (acks=all) sobrevive
     * a la caida de un broker. En local (1 broker) se baja a 1 por variable.
     */
    public record Topicos(
            @DefaultValue("3") @Min(1) @Max(12) int particiones,
            @DefaultValue("3") @Min(1) @Max(3) short replicas,
            @DefaultValue("2") @Min(1) @Max(3) int minInsyncReplicas,
            @DefaultValue("7d") @NotNull Duration retencionActividad,
            @DefaultValue("14d") @NotNull Duration retencionDlt
    ) {

        /** min.insync.replicas nunca puede superar las replicas existentes. */
        public int minInsyncEfectivo() {
            return Math.min(minInsyncReplicas, replicas);
        }
    }

    public record Tendencias(
            /** Largo de la ventana deslizante sobre la que se cuentan las vistas. */
            @DefaultValue("10m") @NotNull Duration ventana,
            @DefaultValue("5") @Min(1) @Max(20) int top,
            /** Hilos consumidores del grupo "tendencias" (uno por particion). */
            @DefaultValue("3") @Min(1) @Max(12) int consumidores,
            /** Costo artificial por evento, solo para hacer visible el lag en la demo. */
            @DefaultValue("0ms") @NotNull Duration costoProcesamiento
    ) {
    }
}
