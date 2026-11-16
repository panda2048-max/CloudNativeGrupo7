package com.example.Timetogame.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Timetogame.dto.HistorialVistaDTO;
import com.example.Timetogame.service.HistorialService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/historial")
@RequiredArgsConstructor
public class HistorialController {

    private final HistorialService historialService;

    @GetMapping("/recientes")
    public ResponseEntity<List<HistorialVistaDTO>> recientes() {
        return ResponseEntity.ok(historialService.recientesDelUsuarioActual());
    }
}
