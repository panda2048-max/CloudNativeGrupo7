package com.example.rabbitadmin.controller;

import static com.example.rabbitadmin.dto.kafka.ReglasTopico.MENSAJE_NOMBRE_EXISTENTE;
import static com.example.rabbitadmin.dto.kafka.ReglasTopico.PATRON_NOMBRE_EXISTENTE;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rabbitadmin.dto.kafka.ActualizarTopicoRequest;
import com.example.rabbitadmin.dto.kafka.CrearTopicoRequest;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.ClusterInfo;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.TopicoDetalle;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.TopicoResumen;
import com.example.rabbitadmin.service.KafkaAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * Cluster y topicos de Kafka.
 * <ul>
 *   <li>GET    /api/admin/kafka/cluster           -> 200 brokers y controlador</li>
 *   <li>GET    /api/admin/kafka/topics            -> 200 lista</li>
 *   <li>GET    /api/admin/kafka/topics/{nombre}   -> 200 particiones (lider, replicas, ISR) y config | 404</li>
 *   <li>POST   /api/admin/kafka/topics            -> 201 + Location | 400 | 409</li>
 *   <li>PATCH  /api/admin/kafka/topics/{nombre}   -> 200 (particiones, retencion, cleanup.policy) | 400 | 404</li>
 *   <li>DELETE /api/admin/kafka/topics/{nombre}   -> 204 | 404 | 409 (protegido)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/kafka")
@RequiredArgsConstructor
public class KafkaTopicAdminController {

    private final KafkaAdminService kafkaAdminService;

    @GetMapping("/cluster")
    public ResponseEntity<ClusterInfo> cluster() {
        return ResponseEntity.ok(kafkaAdminService.describirCluster());
    }

    @GetMapping("/topics")
    public ResponseEntity<List<TopicoResumen>> listar() {
        return ResponseEntity.ok(kafkaAdminService.listarTopicos());
    }

    @GetMapping("/topics/{nombre}")
    public ResponseEntity<TopicoDetalle> obtener(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String nombre) {
        return ResponseEntity.ok(kafkaAdminService.obtenerTopico(nombre));
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicoDetalle> crear(@Valid @RequestBody CrearTopicoRequest request) {
        TopicoDetalle creado = kafkaAdminService.crearTopico(request);
        return ResponseEntity.created(URI.create("/api/admin/kafka/topics/" + creado.nombre())).body(creado);
    }

    @PatchMapping("/topics/{nombre}")
    public ResponseEntity<TopicoDetalle> actualizar(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String nombre,
            @Valid @RequestBody ActualizarTopicoRequest request) {
        return ResponseEntity.ok(kafkaAdminService.actualizarTopico(nombre, request));
    }

    @DeleteMapping("/topics/{nombre}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String nombre) {
        kafkaAdminService.eliminarTopico(nombre);
        return ResponseEntity.noContent().build();
    }
}
