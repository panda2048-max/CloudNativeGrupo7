package com.example.Timetogame.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.Timetogame.entity.Resena;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findTop20ByVideojuegoIdOrderByCreadaEnDesc(Long videojuegoId);

    List<Resena> findTop5ByOrderByCreadaEnDesc();

    long countByVideojuegoId(Long videojuegoId);

    @Query("select avg(r.calificacion) from Resena r where r.videojuegoId = :videojuegoId")
    Double promedioDe(@Param("videojuegoId") Long videojuegoId);

    long deleteByVideojuegoId(Long videojuegoId);
}
