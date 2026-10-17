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

import com.example.rabbitadmin.dto.CrearExchangeRequest;
import com.example.rabbitadmin.dto.ExchangeResponse;
import com.example.rabbitadmin.service.RabbitAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * Exchanges.
 * <ul>
 *   <li>GET    /api/admin/exchanges           -> 200 lista</li>
 *   <li>GET    /api/admin/exchanges/{nombre}  -> 200 | 404</li>
 *   <li>POST   /api/admin/exchanges           -> 201 + Location | 400 | 409</li>
 *   <li>DELETE /api/admin/exchanges/{nombre}?soloSiSinBindings= -> 204 | 404 | 409</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/exchanges")
@RequiredArgsConstructor
public class ExchangeAdminController {

    private final RabbitAdminService rabbitAdminService;

    @GetMapping
    public ResponseEntity<List<ExchangeResponse>> listar() {
        return ResponseEntity.ok(rabbitAdminService.listarExchanges());
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<ExchangeResponse> obtener(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String nombre) {
        return ResponseEntity.ok(rabbitAdminService.obtenerExchange(nombre));
    }

    @PostMapping
    public ResponseEntity<ExchangeResponse> crear(@Valid @RequestBody CrearExchangeRequest request) {
        ExchangeResponse creado = rabbitAdminService.crearExchange(request);
        return ResponseEntity.created(URI.create("/api/admin/exchanges/" + creado.nombre())).body(creado);
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(
            @PathVariable @Pattern(regexp = PATRON_NOMBRE_EXISTENTE, message = MENSAJE_NOMBRE_EXISTENTE) String nombre,
            @RequestParam(defaultValue = "false") boolean soloSiSinBindings) {
        rabbitAdminService.eliminarExchange(nombre, soloSiSinBindings);
        return ResponseEntity.noContent().build();
    }
}
