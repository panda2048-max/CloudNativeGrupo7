package com.example.Timetogame.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Timetogame.entity.Genero;

public interface GeneroRepository extends JpaRepository<Genero, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
