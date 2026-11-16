package com.example.rabbitadmin.dto.kafka;

import static com.example.rabbitadmin.dto.kafka.ReglasTopico.MENSAJE_NOMBRE;
import static com.example.rabbitadmin.dto.kafka.ReglasTopico.PATRON_NOMBRE;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Que el numero de replicas no supere los brokers del cluster se valida en el
 * servicio, contra el cluster real.
 */
public record CrearTopicoRequest(
        @NotBlank(message = "El nombre del topico es obligatorio")
        @Pattern(regexp = PATRON_NOMBRE, message = "Nombre de topico invalido: " + MENSAJE_NOMBRE)
        String nombre,

        @NotNull(message = "Las particiones son obligatorias")
        @Min(value = 1, message = "Debe tener al menos 1 particion")
        @Max(value = 12, message = "Como maximo 12 particiones")
        Integer particiones,

        @NotNull(message = "Las replicas son obligatorias")
        @Min(value = 1, message = "Debe tener al menos 1 replica")
        @Max(value = 3, message = "Como maximo 3 replicas (una por broker)")
        Short replicas,

        /** Por defecto DELETE. */
        PoliticaLimpieza politicaLimpieza,

        /** Opcional (default del broker). -1 = infinita. */
        Long retencionMs,

        /** Opcional. Con acks=all, cuantas replicas deben confirmar una escritura. */
        @Min(value = 1, message = "min.insync.replicas debe ser al menos 1")
        Integer minInsyncReplicas
) {

    public PoliticaLimpieza politicaEfectiva() {
        return politicaLimpieza == null ? PoliticaLimpieza.DELETE : politicaLimpieza;
    }

    @AssertTrue(message = "retencionMs debe ser -1 (infinita) o al menos 60000 (1 minuto)")
    public boolean isRetencionValida() {
        return ReglasTopico.retencionValida(retencionMs);
    }

    @AssertTrue(message = "minInsyncReplicas no puede superar las replicas: ninguna escritura con acks=all se confirmaria")
    public boolean isMinInsyncCoherente() {
        return minInsyncReplicas == null || replicas == null || minInsyncReplicas <= replicas;
    }
}
