package com.example.Timetogame.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.Timetogame.dto.TendenciaDTO;
import com.example.Timetogame.service.TendenciasService;
import com.example.Timetogame.streaming.sse.StreamHub;

import lombok.RequiredArgsConstructor;

/**
 * Tendencias publicas (no requieren sesion, igual que el catalogo).
 * <ul>
 *   <li>{@code /stream/tendencias}: Server-Sent Events, el navegador recibe el
 *       ranking y los avisos del catalogo apenas cambian.</li>
 *   <li>{@code /tendencias}: el ranking actual en JSON, por si el cliente no
 *       puede mantener la conexion abierta.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class StreamController {

    private final StreamHub streamHub;
    private final TendenciasService tendenciasService;

    @GetMapping(path = "/stream/tendencias", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTendencias() {
        return streamHub.suscribir();
    }

    @GetMapping("/tendencias")
    public ResponseEntity<List<TendenciaDTO>> tendencias() {
        return ResponseEntity.ok(tendenciasService.ranking());
    }
}
