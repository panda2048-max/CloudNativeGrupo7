package com.example.Timetogame.streaming.consumer.tendencias;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.Timetogame.service.TendenciasService;
import com.example.Timetogame.streaming.config.TopologiaKafka;
import com.example.Timetogame.streaming.consumer.EventoKafkaValidator;
import com.example.Timetogame.streaming.event.CatalogoCambioEvent;
import com.example.Timetogame.streaming.event.VideojuegoVistoEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer group "tendencias": lee la actividad (con varios hilos, uno por
 * particion) y los cambios del catalogo. Solo valida y delega en
 * TendenciasService; los errores los maneja el DefaultErrorHandler
 * (reintentos con backoff y DLT).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TendenciasConsumer {

    private final EventoKafkaValidator validator;
    private final TendenciasService tendenciasService;

    @KafkaListener(id = "tendencias-actividad", topics = TopologiaKafka.ACTIVIDAD,
            groupId = TopologiaKafka.GRUPO_TENDENCIAS,
            concurrency = "${app.kafka.tendencias.consumidores:3}",
            properties = "spring.json.value.default.type=com.example.Timetogame.streaming.event.VideojuegoVistoEvent")
    public void onVista(ConsumerRecord<String, VideojuegoVistoEvent> record) {
        VideojuegoVistoEvent evento = validator.validar(record);
        if (!tendenciasService.registrarVista(evento)) {
            log.debug("Vista {} ignorada (duplicada o fuera de la ventana): {}", evento.eventId(),
                    EventoKafkaValidator.ubicacion(record));
        }
    }

    @KafkaListener(id = "tendencias-catalogo", topics = TopologiaKafka.CATALOGO_CAMBIOS,
            groupId = TopologiaKafka.GRUPO_TENDENCIAS,
            properties = "spring.json.value.default.type=com.example.Timetogame.streaming.event.CatalogoCambioEvent")
    public void onCambioCatalogo(ConsumerRecord<String, CatalogoCambioEvent> record) {
        CatalogoCambioEvent evento = validator.validar(record);
        tendenciasService.aplicarCambio(evento);
        log.info("Tendencias: {} videojuego {} ({})", evento.tipo(), evento.videojuegoId(),
                EventoKafkaValidator.ubicacion(record));
    }
}
