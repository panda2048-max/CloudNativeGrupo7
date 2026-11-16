package com.example.rabbitadmin.controller;

import static com.example.rabbitadmin.dto.kafka.ReglasTopico.MENSAJE_GRUPO;
import static com.example.rabbitadmin.dto.kafka.ReglasTopico.PATRON_GRUPO;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.GrupoDetalle;
import com.example.rabbitadmin.dto.kafka.KafkaRespuestas.GrupoResumen;
import com.example.rabbitadmin.service.KafkaAdminService;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * Consumer groups.
 * <ul>
 *   <li>GET    /api/admin/kafka/consumer-groups        -> 200 lista con estado</li>
 *   <li>GET    /api/admin/kafka/consumer-groups/{id}   -> 200 miembros, particiones, offsets y lag | 404</li>
 *   <li>DELETE /api/admin/kafka/consumer-groups/{id}   -> 204 | 404 | 409 (tiene consumidores activos)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/kafka/consumer-groups")
@RequiredArgsConstructor
public class KafkaConsumerGroupAdminController {

    private final KafkaAdminService kafkaAdminService;

    @GetMapping
    public ResponseEntity<List<GrupoResumen>> listar() {
        return ResponseEntity.ok(kafkaAdminService.listarGrupos());
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GrupoDetalle> obtener(
            @PathVariable @Pattern(regexp = PATRON_GRUPO, message = MENSAJE_GRUPO) String groupId) {
        return ResponseEntity.ok(kafkaAdminService.obtenerGrupo(groupId));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Pattern(regexp = PATRON_GRUPO, message = MENSAJE_GRUPO) String groupId) {
        kafkaAdminService.eliminarGrupo(groupId);
        return ResponseEntity.noContent().build();
    }
}
