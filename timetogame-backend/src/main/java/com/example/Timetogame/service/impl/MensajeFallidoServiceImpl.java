package com.example.Timetogame.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Timetogame.dto.MensajeFallidoDTO;
import com.example.Timetogame.entity.MensajeFallido;
import com.example.Timetogame.repository.MensajeFallidoRepository;
import com.example.Timetogame.service.MensajeFallidoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MensajeFallidoServiceImpl implements MensajeFallidoService {

    private final MensajeFallidoRepository mensajeFallidoRepository;

    @Override
    public void registrar(MensajeFallidoDTO mensaje) {
        mensajeFallidoRepository.save(MensajeFallido.builder()
                .messageId(mensaje.messageId())
                .colaOrigen(mensaje.colaOrigen())
                .routingKey(mensaje.routingKey())
                .motivo(mensaje.motivo())
                .reintentos(mensaje.reintentos())
                .ultimoError(mensaje.ultimoError())
                .payload(mensaje.payload())
                .registradoEn(Instant.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MensajeFallidoDTO> findRecientes() {
        return mensajeFallidoRepository.findTop50ByOrderByRegistradoEnDesc().stream()
                .map(m -> new MensajeFallidoDTO(m.getId(), m.getMessageId(), m.getColaOrigen(), m.getRoutingKey(),
                        m.getMotivo(), m.getReintentos(), m.getUltimoError(), m.getPayload(), m.getRegistradoEn()))
                .toList();
    }
}
