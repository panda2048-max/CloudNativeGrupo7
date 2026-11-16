package com.example.rabbitadmin.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListGroupsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.stereotype.Service;

import com.example.rabbitadmin.config.KafkaAdminProperties;
import com.example.rabbitadmin.dto.kafka.ActualizarTopicoRequest;
import com.example.rabbitadmin.dto.kafka.CrearTopicoRequest;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.BrokerInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.ClusterInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.GrupoDetalle;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.GrupoResumen;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.MiembroInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.OffsetInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.ParticionInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.TopicoDetalle;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.TopicoResumen;
import com.example.rabbitadmin.exception.BrokerNoDisponibleException;
import com.example.rabbitadmin.exception.ConfiguracionInvalidaException;
import com.example.rabbitadmin.exception.ConflictoTopologiaException;
import com.example.rabbitadmin.exception.RecursoNoEncontradoException;
import com.example.rabbitadmin.service.KafkaAdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Operaciones sobre el cluster con AdminClient. Antes de escribir se valida
 * contra el estado real (brokers disponibles, particiones actuales) para dar
 * errores claros; si igual falla, la excepcion de Kafka se traduce en
 * {@link #traducir}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaAdminServiceImpl implements KafkaAdminService {

    /** Configuracion que se muestra de cada topico (el resto son defaults del broker poco relevantes). */
    private static final Set<String> CONFIG_VISIBLE = Set.of(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.RETENTION_MS_CONFIG, TopicConfig.RETENTION_BYTES_CONFIG,
            TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, TopicConfig.SEGMENT_MS_CONFIG,
            TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG);

    private final AdminClient admin;
    private final KafkaAdminProperties properties;

    // ---------------------------------------------------------------- cluster

    @Override
    public ClusterInfo describirCluster() {
        DescribeClusterResult cluster = admin.describeCluster();
        Collection<Node> nodos = esperar(cluster.nodes(), "Describir el cluster");
        Node controlador = esperar(cluster.controller(), "Describir el cluster");
        Integer controladorId = controlador != null ? controlador.id() : null;
        List<BrokerInfo> brokers = nodos.stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new BrokerInfo(n.id(), n.host(), n.port(), controladorId != null && n.id() == controladorId))
                .toList();
        return new ClusterInfo(esperar(cluster.clusterId(), "Describir el cluster"), controladorId, brokers);
    }

    // ---------------------------------------------------------------- topicos

    @Override
    public List<TopicoResumen> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(true)).names(),
                "Listar topicos");
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames(),
                "Listar topicos");
        return descripciones.values().stream()
                .map(d -> new TopicoResumen(d.name(), d.partitions().size(),
                        d.partitions().isEmpty() ? 0 : d.partitions().getFirst().replicas().size(), d.isInternal()))
                .sorted(Comparator.comparing(TopicoResumen::interno).thenComparing(TopicoResumen::nombre))
                .toList();
    }

    @Override
    public TopicoDetalle obtenerTopico(String nombre) {
        TopicDescription descripcion = esperar(admin.describeTopics(List.of(nombre)).allTopicNames(),
                "Consultar el topico '" + nombre + "'").get(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all(),
                "Consultar el topico '" + nombre + "'").get(recurso);

        List<ParticionInfo> particiones = descripcion.partitions().stream()
                .map(p -> new ParticionInfo(p.partition(), p.leader() != null ? p.leader().id() : null,
                        p.replicas().stream().map(Node::id).toList(), p.isr().stream().map(Node::id).toList()))
                .toList();
        Map<String, String> visible = config.entries().stream()
                .filter(e -> CONFIG_VISIBLE.contains(e.name()))
                .sorted(Comparator.comparing(ConfigEntry::name))
                .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value, (a, b) -> a, LinkedHashMap::new));
        return new TopicoDetalle(nombre, descripcion.isInternal(), particiones, visible);
    }

    @Override
    public TopicoDetalle crearTopico(CrearTopicoRequest request) {
        int brokers = describirCluster().brokers().size();
        if (request.replicas() > brokers) {
            throw new ConfiguracionInvalidaException("No se pueden crear %d replicas: el cluster tiene %d broker(s)"
                    .formatted(request.replicas(), brokers));
        }

        Map<String, String> config = new LinkedHashMap<>();
        config.put(TopicConfig.CLEANUP_POLICY_CONFIG, request.politicaEfectiva().valorKafka());
        if (request.retencionMs() != null) {
            config.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(request.retencionMs()));
        }
        if (request.minInsyncReplicas() != null) {
            config.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(request.minInsyncReplicas()));
        }
        NewTopic topico = new NewTopic(request.nombre(), request.particiones(), request.replicas()).configs(config);
        esperar(admin.createTopics(List.of(topico)).all(), "Crear el topico '" + request.nombre() + "'");
        log.info("Topico creado: {} ({} particiones x {} replicas, {})", request.nombre(), request.particiones(),
                request.replicas(), config);
        return obtenerTopico(request.nombre());
    }

    @Override
    public TopicoDetalle actualizarTopico(String nombre, ActualizarTopicoRequest request) {
        TopicoDetalle actual = obtenerTopico(nombre);

        if (request.particiones() != null) {
            int actuales = actual.particiones().size();
            if (request.particiones() < actuales) {
                throw new ConfiguracionInvalidaException(("No se pueden reducir las particiones de %d a %d: Kafka "
                        + "solo permite aumentarlas").formatted(actuales, request.particiones()));
            }
            if (request.particiones() > actuales) {
                esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(request.particiones()))).all(),
                        "Aumentar las particiones de '" + nombre + "'");
            }
        }

        List<AlterConfigOp> cambios = new ArrayList<>();
        if (request.politicaLimpieza() != null) {
            cambios.add(set(TopicConfig.CLEANUP_POLICY_CONFIG, request.politicaLimpieza().valorKafka()));
        }
        if (request.retencionMs() != null) {
            cambios.add(set(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(request.retencionMs())));
        }
        if (request.minInsyncReplicas() != null) {
            int replicas = actual.particiones().isEmpty() ? 0 : actual.particiones().getFirst().replicas().size();
            if (request.minInsyncReplicas() > replicas) {
                throw new ConfiguracionInvalidaException(("min.insync.replicas=%d supera las %d replicas del topico: "
                        + "ninguna escritura con acks=all se confirmaria").formatted(request.minInsyncReplicas(),
                        replicas));
            }
            cambios.add(set(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(request.minInsyncReplicas())));
        }
        if (!cambios.isEmpty()) {
            ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
            esperar(admin.incrementalAlterConfigs(Map.of(recurso, cambios)).all(),
                    "Actualizar la configuracion de '" + nombre + "'");
        }
        log.info("Topico actualizado: {} -> {}", nombre, request);
        return obtenerTopico(nombre);
    }

    @Override
    public void eliminarTopico(String nombre) {
        if (properties.esProtegido(nombre)) {
            throw new ConflictoTopologiaException("'" + nombre + "' es un topico protegido (del sistema o interno de "
                    + "Kafka) y no se puede eliminar desde esta API");
        }
        esperar(admin.deleteTopics(List.of(nombre)).all(), "Eliminar el topico '" + nombre + "'");
        log.info("Topico eliminado: {}", nombre);
    }

    // ---------------------------------------------------------- consumer groups

    @Override
    public List<GrupoResumen> listarGrupos() {
        return esperar(admin.listGroups(ListGroupsOptions.forConsumerGroups()).all(), "Listar consumer groups").stream()
                .map(g -> new GrupoResumen(g.groupId(), g.groupState().map(Enum::name).orElse("UNKNOWN")))
                .sorted(Comparator.comparing(GrupoResumen::groupId))
                .toList();
    }

    @Override
    public GrupoDetalle obtenerGrupo(String groupId) {
        String operacion = "Consultar el consumer group '" + groupId + "'";
        ConsumerGroupDescription grupo = esperar(admin.describeConsumerGroups(List.of(groupId)).all(), operacion)
                .get(groupId);
        if (grupo == null || ("DEAD".equals(grupo.groupState().name()) && grupo.members().isEmpty()
                && sinOffsets(groupId))) {
            throw new RecursoNoEncontradoException("No existe el consumer group '" + groupId + "'");
        }

        List<MiembroInfo> miembros = grupo.members().stream()
                .map(m -> new MiembroInfo(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream()
                                .sorted(Comparator.comparing(TopicPartition::topic)
                                        .thenComparingInt(TopicPartition::partition))
                                .map(TopicPartition::toString).toList()))
                .sorted(Comparator.comparing(MiembroInfo::clientId))
                .toList();

        Map<TopicPartition, OffsetAndMetadata> consumidos = esperar(
                admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata(), operacion);
        Map<TopicPartition, OffsetSpec> pedido = consumidos.keySet().stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> finales = pedido.isEmpty() ? Map.of()
                : esperar(admin.listOffsets(pedido).all(), operacion);

        List<OffsetInfo> offsets = consumidos.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> {
                    long consumido = e.getValue().offset();
                    long fin = Optional.ofNullable(finales.get(e.getKey())).map(ListOffsetsResultInfo::offset)
                            .orElse(consumido);
                    return new OffsetInfo(e.getKey().topic(), e.getKey().partition(), consumido, fin,
                            Math.max(0, fin - consumido));
                })
                .sorted(Comparator.comparing(OffsetInfo::topico).thenComparingInt(OffsetInfo::particion))
                .toList();

        return new GrupoDetalle(groupId, grupo.groupState().name(),
                grupo.coordinator() != null ? grupo.coordinator().id() : null, grupo.partitionAssignor(), miembros,
                offsets, offsets.stream().mapToLong(OffsetInfo::lag).sum());
    }

    @Override
    public void eliminarGrupo(String groupId) {
        esperar(admin.deleteConsumerGroups(List.of(groupId)).all(),
                "Eliminar el consumer group '" + groupId + "'");
        log.info("Consumer group eliminado: {}", groupId);
    }

    // ---------------------------------------------------------------- soporte

    private boolean sinOffsets(String groupId) {
        return esperar(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata(),
                "Consultar el consumer group '" + groupId + "'").isEmpty();
    }

    private static AlterConfigOp set(String clave, String valor) {
        return new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET);
    }

    private <T> T esperar(KafkaFuture<T> futuro, String operacion) {
        try {
            return futuro.get(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw traducir(operacion, e.getCause());
        } catch (TimeoutException e) {
            throw new BrokerNoDisponibleException(operacion + ": Kafka no respondio a tiempo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BrokerNoDisponibleException(operacion + ": operacion interrumpida", e);
        }
    }

    /** Traduce el error del cluster a uno HTTP con sentido. */
    static RuntimeException traducir(String operacion, Throwable causa) {
        String detalle = operacion + ": " + causa.getMessage();
        return switch (causa) {
            case TopicExistsException _ -> new ConflictoTopologiaException(detalle);
            case GroupNotEmptyException _ -> new ConflictoTopologiaException(operacion
                    + ": el grupo tiene consumidores activos; detenlos antes de eliminarlo");
            case UnknownTopicOrPartitionException _ -> new RecursoNoEncontradoException(operacion
                    + ": el topico no existe");
            case GroupIdNotFoundException _ -> new RecursoNoEncontradoException(operacion + ": el grupo no existe");
            // Incluye InvalidReplicationFactorException e InvalidTopicException (subclases).
            case InvalidConfigurationException _ -> new ConfiguracionInvalidaException(detalle);
            case InvalidPartitionsException _ -> new ConfiguracionInvalidaException(detalle);
            case PolicyViolationException _ -> new ConfiguracionInvalidaException(detalle);
            case org.apache.kafka.common.errors.TimeoutException e ->
                    new BrokerNoDisponibleException(operacion + ": Kafka no disponible", e);
            default -> new BrokerNoDisponibleException(detalle, causa);
        };
    }
}
