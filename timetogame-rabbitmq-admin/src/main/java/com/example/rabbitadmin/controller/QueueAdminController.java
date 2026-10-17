package com.example.rabbitadmin.controller;

import static com.example.rabbitadmin.dto.ReglasNombre.MENSAJE_NOMBRE_EXISTENTE;
import static com.example.rabbitadmin.dto.ReglasNombre.PATRON_NOMBRE_EXISTENTE;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.rabbitadmin.dto.ColaResponse;
import com.example.rabbitadmin.dto.CrearColaRequest;
import com.example.rabbitadmin.service.RabbitAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * Colas.
 * <ul>
 *   <li>GET    /api/admin/queues           -> 200 lista</li>
 *   <li>GET    /api/admin/queues/{nombre}  -> 200 | 404</li>
 *   <li>POST   /api/admin/queues           -> 201 + Location | 400 | 409</li>
 *   <li>DELETE /api/admin/queues/{nombre}?soloSiSinConsumidores=&amp;soloSiVacia= -> 204 | 404 | 409</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/queues")
@RequiredArgsConstructor
public class QueueAdminController {

    private final RabbitAdminService rabbitAdminService;

    @GetMapping
    public ResponseEntity<List<ColaResponse>> listar() {
        return ResponseEntity.ok(rabbitAdminService.listarColas());
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<ColaResponse> obtener(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String nombre) {
        return ResponseEntity.ok(rabbitAdminService.obtenerCola(nombre));
    }

    @PostMapping
    public ResponseEntity<ColaResponse> crear(@Valid @RequestBody CrearColaRequest request) {
        ColaResponse creada = rabbitAdminService.crearCola(request);
        return ResponseEntity.created(URI.create("/api/admin/queues/" + creada.nombre())).body(creada);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String nombre,
            @RequestParam(defaultValue = "false") boolean soloSiSinConsumidores,
            @RequestParam(defaultValue = "false") boolean soloSiVacia) {
        rabbitAdminService.eliminarCola(nombre, soloSiSinConsumidores, soloSiVacia);
        return ResponseEntity.noContent().build();
    }
}
