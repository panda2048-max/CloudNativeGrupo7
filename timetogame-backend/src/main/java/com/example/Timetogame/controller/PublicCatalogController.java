package com.example.Timetogame.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Timetogame.dto.GeneroDTO;
import com.example.Timetogame.dto.ResumenResenasDTO;
import com.example.Timetogame.dto.VideojuegoDTO;
import com.example.Timetogame.service.ActividadService;
import com.example.Timetogame.service.GeneroService;
import com.example.Timetogame.service.ResenaService;
import com.example.Timetogame.service.VideojuegoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final VideojuegoService videojuegoService;
    private final GeneroService generoService;
    private final ActividadService actividadService;
    private final ResenaService resenaService;

    @GetMapping("/videojuegos")
    public ResponseEntity<List<VideojuegoDTO>> findAllVideojuegos() {
        return ResponseEntity.ok(videojuegoService.findAll());
    }

    /** Abrir la ficha cuenta como una vista (evento de actividad para Kafka). */
    @GetMapping("/videojuegos/{id}")
    public ResponseEntity<VideojuegoDTO> findVideojuegoById(@PathVariable Long id) {
        VideojuegoDTO videojuego = videojuegoService.findById(id);
        actividadService.registrarVista(videojuego);
        return ResponseEntity.ok(videojuego);
    }

    /** Promedio, total y ultimas resenas de un videojuego (publico, como el catalogo). */
    @GetMapping("/videojuegos/{id}/resenas")
    public ResponseEntity<ResumenResenasDTO> findResenas(@PathVariable Long id) {
        return ResponseEntity.ok(resenaService.resumenDe(id));
    }

    @GetMapping("/generos")
    public ResponseEntity<List<GeneroDTO>> findAllGeneros() {
        return ResponseEntity.ok(generoService.findAll());
    }
}
