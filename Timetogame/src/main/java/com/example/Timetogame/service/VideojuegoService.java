package com.example.Timetogame.service;

import java.util.List;

import com.example.Timetogame.dto.VideojuegoDTO;

public interface VideojuegoService {

    List<VideojuegoDTO> findAll();

    VideojuegoDTO findById(Long id);

    List<VideojuegoDTO> findByGeneroId(Long generoId);

    VideojuegoDTO create(VideojuegoDTO videojuegoDTO);

    VideojuegoDTO update(Long id, VideojuegoDTO videojuegoDTO);

    void delete(Long id);
}
