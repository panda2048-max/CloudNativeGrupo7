package com.example.Timetogame.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Timetogame.dto.SimulacionDTO;
import com.example.Timetogame.service.ActividadService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** Generador de carga para la demo de streaming (solo ADMIN). */
@RestController
@RequestMapping("/api/streaming")
@RequiredArgsConstructor
public class SimulacionController {

    private final ActividadService actividadService;

    /** 202: las vistas se encolan en Kafka y se procesan despues; el ranking se actualiza en vivo. */
    @PostMapping("/simulacion")
    public ResponseEntity<SimulacionDTO> simular(
            @RequestParam(defaultValue = "200")
            @Min(value = 1, message = "Debe simular al menos 1 vista")
            @Max(value = 5000, message = "Como maximo 5000 vistas por simulacion") int vistas) {
        return ResponseEntity.accepted().body(actividadService.simularVistas(vistas));
    }
}
