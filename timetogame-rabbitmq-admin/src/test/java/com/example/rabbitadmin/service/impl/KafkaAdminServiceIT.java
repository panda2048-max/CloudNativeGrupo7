package com.example.rabbitadmin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.example.rabbitadmin.config.KafkaAdminProperties;
import com.example.rabbitadmin.dto.kafka.ActualizarTopicoRequest;
import com.example.rabbitadmin.dto.kafka.CrearTopicoRequest;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.TopicoDetalle;
import com.example.rabbitadmin.dto.kafka.PoliticaLimpieza;
import com.example.rabbitadmin.exception.ConfiguracionInvalidaException;
import com.example.rabbitadmin.exception.ConflictoTopologiaException;
import com.example.rabbitadmin.exception.RecursoNoEncontradoException;

/**
 * Prueba el servicio contra un Kafka real en localhost:9092 (1 broker). No
 * corre en un build normal:
 * <pre>./mvnw test -Dtest=KafkaAdminServiceIT -Dkafka.it=true</pre>
 * Ojo con un broker nativo en Windows: eliminar un topico (lo hace esta
 * prueba) choca con el bloqueo de archivos de Windows y el broker se apaga
 * ("all log dirs have failed"). Correrla contra el cluster en Linux/Docker, o
 * borrar a mano la carpeta del topico en log.dirs y reiniciar el broker.
 */
@EnabledIfSystemProperty(named = "kafka.it", matches = "true")
class KafkaAdminServiceIT {

    private static AdminClient admin;
    private static KafkaAdminServiceImpl service;
    private static final String TOPICO = "prueba-it-" + UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    static void setUp() {
        admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"));
        service = new KafkaAdminServiceImpl(admin,
                new KafkaAdminProperties(List.of("timetogame.", "__"), Duration.ofSeconds(10)));
    }

    @AfterAll
    static void tearDown() {
        try {
            service.eliminarTopico(TOPICO);
        } catch (RuntimeException ignorada) {
            // ya eliminado por la prueba
        }
        admin.close();
    }

    @Test
    void cicloCompletoDeUnTopico() {
        assertThat(service.describirCluster().brokers()).isNotEmpty();

        TopicoDetalle creado = service.crearTopico(new CrearTopicoRequest(TOPICO, 2, (short) 1,
                PoliticaLimpieza.DELETE, 3_600_000L, 1));
        assertThat(creado.particiones()).hasSize(2);
        assertThat(creado.configuracion()).containsEntry("retention.ms", "3600000");

        assertThatThrownBy(() -> service.crearTopico(new CrearTopicoRequest(TOPICO, 1, (short) 1, null, null, null)))
                .isInstanceOf(ConflictoTopologiaException.class);
        assertThatThrownBy(() -> service.crearTopico(new CrearTopicoRequest(TOPICO + "-x", 1, (short) 3, null, null,
                null)))
                .isInstanceOf(ConfiguracionInvalidaException.class)
                .hasMessageContaining("broker");

        TopicoDetalle actualizado = service.actualizarTopico(TOPICO,
                new ActualizarTopicoRequest(3, PoliticaLimpieza.COMPACT, 7_200_000L, null));
        assertThat(actualizado.particiones()).hasSize(3);
        assertThat(actualizado.configuracion())
                .containsEntry("cleanup.policy", "compact")
                .containsEntry("retention.ms", "7200000");

        assertThatThrownBy(() -> service.actualizarTopico(TOPICO, new ActualizarTopicoRequest(1, null, null, null)))
                .isInstanceOf(ConfiguracionInvalidaException.class)
                .hasMessageContaining("reducir");

        assertThat(service.listarTopicos()).anyMatch(t -> t.nombre().equals(TOPICO));

        service.eliminarTopico(TOPICO);
        assertThatThrownBy(() -> service.obtenerTopico(TOPICO)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void protegeLosTopicosDelSistema() {
        assertThatThrownBy(() -> service.eliminarTopico("timetogame.actividad"))
                .isInstanceOf(ConflictoTopologiaException.class);
        assertThatThrownBy(() -> service.eliminarTopico("__consumer_offsets"))
                .isInstanceOf(ConflictoTopologiaException.class);
    }

    @Test
    void describeLosGruposDelBackend() {
        assertThat(service.listarGrupos()).extracting(g -> g.groupId()).contains("tendencias", "historial");
        assertThat(service.obtenerGrupo("historial").offsets()).isNotEmpty();
        assertThatThrownBy(() -> service.obtenerGrupo("grupo-que-no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
