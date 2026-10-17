package com.example.Timetogame.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Timetogame.entity.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    boolean existsByEventId(String eventId);

    List<Notificacion> findTop50ByOrderByProcesadoEnDesc();
}
