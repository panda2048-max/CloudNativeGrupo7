package com.example.rabbitadmin.dto.kafka;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Cambios sobre un topico existente; los campos nulos no se tocan. Las
 * particiones solo pueden aumentar (Kafka no permite reducirlas: romperia el
 * orden por key) y las replicas no se cambian desde aqui (requiere reasignar
 * particiones entre brokers).
 */
public record ActualizarTopicoRequest(
        @Min(value = 1, message = "Debe tener al menos 1 particion")
        @Max(value = 12, message = "Como maximo 12 particiones")
        Integer particiones,

        PoliticaLimpieza politicaLimpieza,

        Long retencionMs,

        @Min(value = 1, message = "min.insync.replicas debe ser al menos 1")
        @Max(value = 3, message = "min.insync.replicas no puede superar 3")
        Integer minInsyncReplicas
) {

    @AssertTrue(message = "Indique al menos un cambio: particiones, politicaLimpieza, retencionMs o minInsyncReplicas")
    public boolean isAlgunCambio() {
        return particiones != null || politicaLimpieza != null || retencionMs != null || minInsyncReplicas != null;
    }

    @AssertTrue(message = "retencionMs debe ser -1 (infinita) o al menos 60000 (1 minuto)")
    public boolean isRetencionValida() {
        return ReglasTopico.retencionValida(retencionMs);
    }
}
