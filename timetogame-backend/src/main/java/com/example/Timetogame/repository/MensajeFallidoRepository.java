package com.example.Timetogame.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Timetogame.entity.MensajeFallido;

public interface MensajeFallidoRepository extends JpaRepository<MensajeFallido, Long> {

    List<MensajeFallido> findTop50ByOrderByRegistradoEnDesc();
}
