package com.example.Timetogame.entity;

import java.time.Instant;

import com.example.Timetogame.event.EntidadCatalogo;
import com.example.Timetogame.event.TipoEventoCatalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unico: RabbitMQ garantiza entrega "al menos una vez", esto evita duplicados. */
    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 20)
    private TipoEventoCatalogo tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntidadCatalogo entidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(nullable = false, length = 300)
    private String mensaje;

    @Column(length = 120)
    private String usuario;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;
}
