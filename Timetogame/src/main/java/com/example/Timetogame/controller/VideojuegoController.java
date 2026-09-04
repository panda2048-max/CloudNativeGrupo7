package com.example.Timetogame.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Timetogame.dto.VideojuegoDTO;
import com.example.Timetogame.service.VideojuegoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/videojuegos")
@RequiredArgsConstructor
public class VideojuegoController {

    private final VideojuegoService videojuegoService;

    @GetMapping
    public ResponseEntity<List<VideojuegoDTO>> findAll(
            @RequestParam(required = false) Long generoId) {
        if (generoId != null) {
            return ResponseEntity.ok(videojuegoService.findByGeneroId(generoId));
        }
        return ResponseEntity.ok(videojuegoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideojuegoDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(videojuegoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<VideojuegoDTO> create(@Valid @RequestBody VideojuegoDTO videojuegoDTO) {
        VideojuegoDTO creado = videojuegoService.create(videojuegoDTO);
        return ResponseEntity.created(URI.create("/api/videojuegos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VideojuegoDTO> update(@PathVariable Long id, @Valid @RequestBody VideojuegoDTO videojuegoDTO) {
        return ResponseEntity.ok(videojuegoService.update(id, videojuegoDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        videojuegoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
