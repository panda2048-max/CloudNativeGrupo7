package com.example.rabbitadmin.dto.kafka;

import java.util.List;
import java.util.Map;

/** Respuestas de la API de administracion de Kafka (records agrupados por cercania). */
public final class KafkaRespuestas {

    private KafkaRespuestas() {
    }

    /** GET /api/admin/kafka/cluster: los brokers vivos y cual es el controlador. */
    public record ClusterInfo(
            String clusterId,
            Integer controladorId,
            List<BrokerInfo> brokers
    ) {
    }

    public record BrokerInfo(
            int id,
            String host,
            int puerto,
            boolean controlador
    ) {
    }

    public record TopicoResumen(
            String nombre,
            int particiones,
            int replicas,
            boolean interno
    ) {
    }

    /**
     * @param particiones por particion: lider, replicas e ISR (replicas al dia). Si un
     *                    broker cae, otra replica del ISR pasa a ser lider.
     * @param configuracion las propiedades relevantes del topico (cleanup.policy, retention.ms...)
     */
    public record TopicoDetalle(
            String nombre,
            boolean interno,
            List<ParticionInfo> particiones,
            Map<String, String> configuracion
    ) {
    }

    public record ParticionInfo(
            int particion,
            Integer lider,
            List<Integer> replicas,
            List<Integer> isr
    ) {
    }

    public record GrupoResumen(
            String groupId,
            String estado
    ) {
    }

    /**
     * @param miembros consumidores activos y las particiones que tiene cada uno
     * @param offsets  por particion: hasta donde leyo el grupo, el final del log y la diferencia (lag)
     * @param lagTotal mensajes pendientes del grupo; si crece sostenidamente el consumidor no da abasto
     */
    public record GrupoDetalle(
            String groupId,
            String estado,
            Integer coordinador,
            String asignador,
            List<MiembroInfo> miembros,
            List<OffsetInfo> offsets,
            long lagTotal
    ) {
    }

    public record MiembroInfo(
            String consumerId,
            String clientId,
            String host,
            List<String> particiones
    ) {
    }

    public record OffsetInfo(
            String topico,
            int particion,
            long offsetConsumido,
            long offsetFinal,
            long lag
    ) {
    }
}
