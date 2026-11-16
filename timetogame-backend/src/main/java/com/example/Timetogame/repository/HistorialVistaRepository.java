package com.example.Timetogame.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Timetogame.entity.HistorialVista;

public interface HistorialVistaRepository extends JpaRepository<HistorialVista, Long> {

    boolean existsByEventId(String eventId);

    List<HistorialVista> findTop10ByUsuarioOrderByVistoEnDesc(String usuario);
}
