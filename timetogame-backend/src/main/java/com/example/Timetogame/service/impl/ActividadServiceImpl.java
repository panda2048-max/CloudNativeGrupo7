package com.example.Timetogame.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.example.Timetogame.dto.SimulacionDTO;
import com.example.Timetogame.dto.VideojuegoDTO;
import com.example.Timetogame.exception.ResourceNotFoundException;
import com.example.Timetogame.security.UsuarioActualProvider;
import com.example.Timetogame.service.ActividadService;
import com.example.Timetogame.service.VideojuegoService;
import com.example.Timetogame.streaming.event.OrigenActividad;
import com.example.Timetogame.streaming.event.ActividadEvent;

import lombok.RequiredArgsConstructor;

/**
 * Publica cada vista como evento de Spring; ActividadProducer la lleva a Kafka.
 * Este servicio no conoce Kafka.
 */
@Service
@RequiredArgsConstructor
public class ActividadServiceImpl implements ActividadService {

    static final String USUARIO_SIMULADOR = "simulador";

    private final ApplicationEventPublisher eventPublisher;
    private final UsuarioActualProvider usuarioActualProvider;
    private final VideojuegoService videojuegoService;

    @Override
    public void registrarVista(VideojuegoDTO videojuego) {
        eventPublisher.publishEvent(ActividadEvent.vista(videojuego.id(), videojuego.titulo(),
                usuarioActualProvider.usuarioActual(), OrigenActividad.DETALLE));
    }

    @Override
    public SimulacionDTO simularVistas(int cantidad) {
        List<VideojuegoDTO> videojuegos = videojuegoService.findAll();
        if (videojuegos.isEmpty()) {
            throw new ResourceNotFoundException("No hay videojuegos en el catalogo para simular vistas");
        }
        double[] pesosAcumulados = pesosZipf(videojuegos.size());

        Map<String, Integer> porVideojuego = new LinkedHashMap<>();
        for (int i = 0; i < cantidad; i++) {
            VideojuegoDTO elegido = videojuegos.get(elegir(pesosAcumulados));
            eventPublisher.publishEvent(ActividadEvent.vista(elegido.id(), elegido.titulo(), USUARIO_SIMULADOR,
                    OrigenActividad.SIMULACION));
            porVideojuego.merge(elegido.titulo(), 1, Integer::sum);
        }
        return new SimulacionDTO(cantidad, porVideojuego);
    }

    /** El juego en la posicion i recibe un peso 1/(i+1): pocos juegos concentran la mayoria de vistas. */
    private static double[] pesosZipf(int n) {
        double[] acumulados = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            total += 1.0 / (i + 1);
            acumulados[i] = total;
        }
        for (int i = 0; i < n; i++) {
            acumulados[i] /= total;
        }
        return acumulados;
    }

    private static int elegir(double[] pesosAcumulados) {
        double r = ThreadLocalRandom.current().nextDouble();
        for (int i = 0; i < pesosAcumulados.length; i++) {
            if (r <= pesosAcumulados[i]) {
                return i;
            }
        }
        return pesosAcumulados.length - 1;
    }
}
