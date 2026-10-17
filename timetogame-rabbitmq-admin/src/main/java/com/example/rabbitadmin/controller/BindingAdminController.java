package com.example.rabbitadmin.controller;

import static com.example.rabbitadmin.dto.ReglasNombre.MENSAJE_NOMBRE_EXISTENTE;
import static com.example.rabbitadmin.dto.ReglasNombre.MENSAJE_ROUTING_KEY;
import static com.example.rabbitadmin.dto.ReglasNombre.PATRON_NOMBRE_EXISTENTE;
import static com.example.rabbitadmin.dto.ReglasNombre.PATRON_ROUTING_KEY;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.rabbitadmin.dto.BindingResponse;
import com.example.rabbitadmin.dto.CrearBindingRequest;
import com.example.rabbitadmin.service.RabbitAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * Bindings (exchange -> cola). Un binding se identifica por exchange, cola y
 * routing key; la routing key va como query param porque puede estar vacia o
 * contener "#", que en el path se interpretaria como fragmento.
 * <ul>
 *   <li>GET    /api/admin/bindings                                  -> 200 lista</li>
 *   <li>POST   /api/admin/bindings                                  -> 201 | 400 | 404 | 409</li>
 *   <li>DELETE /api/admin/bindings/{exchange}/{cola}?routingKey=    -> 204 | 404 | 409</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/bindings")
@RequiredArgsConstructor
public class BindingAdminController {

    private final RabbitAdminService rabbitAdminService;

    @GetMapping
    public ResponseEntity<List<BindingResponse>> listar() {
        return ResponseEntity.ok(rabbitAdminService.listarBindings());
    }

    @PostMapping
    public ResponseEntity<BindingResponse> crear(@Valid @RequestBody CrearBindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.crearBinding(request));
    }

    @DeleteMapping("/{exchange}/{cola}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String exchange,
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String cola,
            @RequestParam(defaultValue = "")
            @Pattern(regexp = PATRON_ROUTING_KEY, message = "Routing key invalida: " + MENSAJE_ROUTING_KEY)
            String routingKey) {
        rabbitAdminService.eliminarBinding(exchange, cola, routingKey);
        return ResponseEntity.noContent().build();
    }
}
