package com.example.Timetogame.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Timetogame.entity.Videojuego;

public interface VideojuegoRepository extends JpaRepository<Videojuego, Long> {

    List<Videojuego> findByGeneroId(Long generoId);
}
