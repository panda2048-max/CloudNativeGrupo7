package com.example.Timetogame.service;

import java.util.List;

import com.example.Timetogame.dto.GeneroDTO;

public interface GeneroService {

    List<GeneroDTO> findAll();

    GeneroDTO findById(Long id);

    GeneroDTO create(GeneroDTO generoDTO);

    GeneroDTO update(Long id, GeneroDTO generoDTO);

    void delete(Long id);
}
