package com.example.Timetogame.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Timetogame.dto.GeneroDTO;
import com.example.Timetogame.dto.VideojuegoDTO;
import com.example.Timetogame.service.GeneroService;
import com.example.Timetogame.service.VideojuegoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final VideojuegoService videojuegoService;
    private final GeneroService generoService;

    @GetMapping("/videojuegos")
    public ResponseEntity<List<VideojuegoDTO>> findAllVideojuegos() {
        return ResponseEntity.ok(videojuegoService.findAll());
    }

    @GetMapping("/videojuegos/{id}")
    public ResponseEntity<VideojuegoDTO> findVideojuegoById(@PathVariable Long id) {
        return ResponseEntity.ok(videojuegoService.findById(id));
    }

    @GetMapping("/generos")
    public ResponseEntity<List<GeneroDTO>> findAllGeneros() {
        return ResponseEntity.ok(generoService.findAll());
    }
}
