package com.example.Timetogame.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Timetogame.entity.FichaVideojuego;

public interface FichaVideojuegoRepository extends JpaRepository<FichaVideojuego, Long> {

    Optional<FichaVideojuego> findByVideojuegoId(Long videojuegoId);

    long deleteByVideojuegoId(Long videojuegoId);
}
