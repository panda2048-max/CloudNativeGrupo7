package com.example.Timetogame.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Timetogame.dto.MensajeFallidoDTO;
import com.example.Timetogame.service.MensajeFallidoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mensajes-fallidos")
@RequiredArgsConstructor
public class MensajeFallidoController {

    private final MensajeFallidoService mensajeFallidoService;

    @GetMapping
    public ResponseEntity<List<MensajeFallidoDTO>> findRecientes() {
        return ResponseEntity.ok(mensajeFallidoService.findRecientes());
    }
}
