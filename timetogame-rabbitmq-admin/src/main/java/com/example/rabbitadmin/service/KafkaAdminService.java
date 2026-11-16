package com.example.rabbitadmin.service;

import java.util.List;

import com.example.rabbitadmin.dto.kafka.ActualizarTopicoRequest;
import com.example.rabbitadmin.dto.kafka.CrearTopicoRequest;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.ClusterInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.GrupoDetalle;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.GrupoResumen;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.TopicoDetalle;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.TopicoResumen;

/**
 * Administracion del cluster Kafka. Unica clase que habla con AdminClient:
 * los controladores solo traducen HTTP.
 */
public interface KafkaAdminService {

    ClusterInfo describirCluster();

    List<TopicoResumen> listarTopicos();

    TopicoDetalle obtenerTopico(String nombre);

    TopicoDetalle crearTopico(CrearTopicoRequest request);

    TopicoDetalle actualizarTopico(String nombre, ActualizarTopicoRequest request);

    void eliminarTopico(String nombre);

    List<GrupoResumen> listarGrupos();

    /** Miembros, reparto de particiones, offsets y lag del grupo. */
    GrupoDetalle obtenerGrupo(String groupId);

    /** Solo si el grupo no tiene consumidores activos (lo exige Kafka). */
    void eliminarGrupo(String groupId);
}
