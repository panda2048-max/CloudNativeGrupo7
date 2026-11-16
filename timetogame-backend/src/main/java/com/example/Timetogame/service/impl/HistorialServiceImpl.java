package com.example.Timetogame.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Timetogame.dto.HistorialVistaDTO;
import com.example.Timetogame.entity.HistorialVista;
import com.example.Timetogame.repository.HistorialVistaRepository;
import com.example.Timetogame.security.UsuarioActualProvider;
import com.example.Timetogame.service.HistorialService;
import com.example.Timetogame.streaming.event.OrigenActividad;
import com.example.Timetogame.streaming.event.ActividadEvent;
import com.example.Timetogame.streaming.event.TipoActividad;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HistorialServiceImpl implements HistorialService {

    private final HistorialVistaRepository repository;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    public boolean registrar(ActividadEvent evento) {
        // Solo vistas reales de usuarios con sesion (no resenas, visitantes ni simulacion).
        if (evento.tipo() != TipoActividad.VISTA || evento.usuario() == null
                || evento.origen() == OrigenActividad.SIMULACION) {
            return false;
        }
        if (repository.existsByEventId(evento.eventId())) {
            return false;
        }
        repository.save(HistorialVista.builder()
                .eventId(evento.eventId())
                .videojuegoId(evento.videojuegoId())
                .titulo(evento.titulo())
                .usuario(evento.usuario())
                .vistoEn(evento.ocurridoEn())
                .registradoEn(Instant.now())
                .build());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialVistaDTO> recientesDelUsuarioActual() {
        String usuario = usuarioActualProvider.usuarioActual();
        if (usuario == null) {
            return List.of();
        }
        return repository.findTop10ByUsuarioOrderByVistoEnDesc(usuario).stream()
                .map(h -> new HistorialVistaDTO(h.getVideojuegoId(), h.getTitulo(), h.getVistoEn()))
                .toList();
    }
}
