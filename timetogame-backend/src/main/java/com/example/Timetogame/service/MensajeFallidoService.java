package com.example.Timetogame.service;

import java.util.List;

import com.example.Timetogame.dto.MensajeFallidoDTO;

public interface MensajeFallidoService {

    void registrar(MensajeFallidoDTO mensaje);

    List<MensajeFallidoDTO> findRecientes();
}
