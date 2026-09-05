package com.example.Timetogame.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Timetogame.dto.GeneroDTO;
import com.example.Timetogame.entity.Genero;
import com.example.Timetogame.exception.ResourceNotFoundException;
import com.example.Timetogame.repository.GeneroRepository;
import com.example.Timetogame.service.GeneroService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GeneroServiceImpl implements GeneroService {

    private final GeneroRepository generoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GeneroDTO> findAll() {
        return generoRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GeneroDTO findById(Long id) {
        return toDTO(buscarEntidad(id));
    }

    @Override
    public GeneroDTO create(GeneroDTO generoDTO) {
        if (generoRepository.existsByNombreIgnoreCase(generoDTO.nombre())) {
            throw new IllegalArgumentException("Ya existe un genero con el nombre '" + generoDTO.nombre() + "'");
        }

        Genero genero = Genero.builder()
                .nombre(generoDTO.nombre())
                .build();

        return toDTO(generoRepository.save(genero));
    }

    @Override
    public GeneroDTO update(Long id, GeneroDTO generoDTO) {
        Genero genero = buscarEntidad(id);
        genero.setNombre(generoDTO.nombre());
        return toDTO(generoRepository.save(genero));
    }

    @Override
    public void delete(Long id) {
        Genero genero = buscarEntidad(id);
        generoRepository.delete(genero);
    }

    private Genero buscarEntidad(Long id) {
        return generoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genero no encontrado con id " + id));
    }

    private GeneroDTO toDTO(Genero genero) {
        return new GeneroDTO(genero.getId(), genero.getNombre());
    }
}
