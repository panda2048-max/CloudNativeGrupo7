package com.example.Timetogame.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Timetogame.dto.NotificacionDTO;
import com.example.Timetogame.service.NotificacionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> findRecientes() {
        return ResponseEntity.ok(notificacionService.findRecientes());
    }
}
