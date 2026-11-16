package com.example.Timetogame.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Timetogame.dto.CrearResenaRequest;
import com.example.Timetogame.dto.ResenaDTO;
import com.example.Timetogame.dto.ResumenResenasDTO;
import com.example.Timetogame.dto.VideojuegoDTO;
import com.example.Timetogame.entity.Resena;
import com.example.Timetogame.repository.ResenaRepository;
import com.example.Timetogame.security.UsuarioActualProvider;
import com.example.Timetogame.service.ResenaService;
import com.example.Timetogame.service.VideojuegoService;
import com.example.Timetogame.streaming.event.ActividadEvent;

import lombok.RequiredArgsConstructor;

/**
 * La resena se guarda en la BD (fuente de verdad) y se publica como evento de
 * Spring; ActividadProducer la lleva a Kafka despues del commit. Este servicio
 * no conoce Kafka.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ResenaServiceImpl implements ResenaService {

    private final ResenaRepository resenaRepository;
    private final VideojuegoService videojuegoService;
    private final UsuarioActualProvider usuarioActualProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ResenaDTO crear(Long videojuegoId, CrearResenaRequest request) {
        VideojuegoDTO videojuego = videojuegoService.findById(videojuegoId);
        String usuario = usuarioActualProvider.usuarioActual();
        if (usuario == null) {
            throw new IllegalArgumentException("Debes iniciar sesion para publicar una resena");
        }

        Resena resena = resenaRepository.save(Resena.builder()
                .videojuegoId(videojuego.id())
                .tituloVideojuego(videojuego.titulo())
                .usuario(usuario)
                .calificacion(request.calificacion())
                .comentario(request.comentario().strip())
                .creadaEn(Instant.now())
                .build());

        eventPublisher.publishEvent(ActividadEvent.resena(resena.getVideojuegoId(), resena.getTituloVideojuego(),
                resena.getUsuario(), resena.getCalificacion(), resena.getComentario(), resena.getCreadaEn()));
        return toDTO(resena);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenResenasDTO resumenDe(Long videojuegoId) {
        videojuegoService.findById(videojuegoId);
        Double promedio = resenaRepository.promedioDe(videojuegoId);
        return new ResumenResenasDTO(
                promedio != null ? Math.round(promedio * 10) / 10.0 : null,
                resenaRepository.countByVideojuegoId(videojuegoId),
                resenaRepository.findTop20ByVideojuegoIdOrderByCreadaEnDesc(videojuegoId).stream()
                        .map(ResenaServiceImpl::toDTO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResenaDTO> recientes() {
        return resenaRepository.findTop5ByOrderByCreadaEnDesc().stream().map(ResenaServiceImpl::toDTO).toList();
    }

    private static ResenaDTO toDTO(Resena r) {
        return new ResenaDTO(r.getId(), r.getVideojuegoId(), r.getTituloVideojuego(), r.getUsuario(),
                r.getCalificacion(), r.getComentario(), r.getCreadaEn());
    }
}
