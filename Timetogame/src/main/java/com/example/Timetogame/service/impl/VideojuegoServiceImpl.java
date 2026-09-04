package com.example.Timetogame.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Timetogame.dto.VideojuegoDTO;
import com.example.Timetogame.entity.Genero;
import com.example.Timetogame.entity.Videojuego;
import com.example.Timetogame.exception.ResourceNotFoundException;
import com.example.Timetogame.repository.GeneroRepository;
import com.example.Timetogame.repository.VideojuegoRepository;
import com.example.Timetogame.service.VideojuegoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VideojuegoServiceImpl implements VideojuegoService {

    private final VideojuegoRepository videojuegoRepository;
    private final GeneroRepository generoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VideojuegoDTO> findAll() {
        return videojuegoRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VideojuegoDTO findById(Long id) {
        return toDTO(buscarEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideojuegoDTO> findByGeneroId(Long generoId) {
        return videojuegoRepository.findByGeneroId(generoId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public VideojuegoDTO create(VideojuegoDTO videojuegoDTO) {
        Genero genero = buscarGenero(videojuegoDTO.generoId());

        Videojuego videojuego = Videojuego.builder()
                .titulo(videojuegoDTO.titulo())
                .descripcion(videojuegoDTO.descripcion())
                .precio(videojuegoDTO.precio())
                .fechaLanzamiento(videojuegoDTO.fechaLanzamiento())
                .genero(genero)
                .build();

        return toDTO(videojuegoRepository.save(videojuego));
    }

    @Override
    public VideojuegoDTO update(Long id, VideojuegoDTO videojuegoDTO) {
        Videojuego videojuego = buscarEntidad(id);
        Genero genero = buscarGenero(videojuegoDTO.generoId());

        videojuego.setTitulo(videojuegoDTO.titulo());
        videojuego.setDescripcion(videojuegoDTO.descripcion());
        videojuego.setPrecio(videojuegoDTO.precio());
        videojuego.setFechaLanzamiento(videojuegoDTO.fechaLanzamiento());
        videojuego.setGenero(genero);

        return toDTO(videojuegoRepository.save(videojuego));
    }

    @Override
    public void delete(Long id) {
        Videojuego videojuego = buscarEntidad(id);
        videojuegoRepository.delete(videojuego);
    }

    private Videojuego buscarEntidad(Long id) {
        return videojuegoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Videojuego no encontrado con id " + id));
    }

    private Genero buscarGenero(Long generoId) {
        return generoRepository.findById(generoId)
                .orElseThrow(() -> new ResourceNotFoundException("Genero no encontrado con id " + generoId));
    }

    private VideojuegoDTO toDTO(Videojuego videojuego) {
        return new VideojuegoDTO(
                videojuego.getId(),
                videojuego.getTitulo(),
                videojuego.getDescripcion(),
                videojuego.getPrecio(),
                videojuego.getFechaLanzamiento(),
                videojuego.getGenero().getId(),
                videojuego.getGenero().getNombre()
        );
    }
}
