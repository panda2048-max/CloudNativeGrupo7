package com.example.Timetogame.streaming.config;

/**
 * Unico lugar con los nombres de topicos y consumer groups de Kafka del backend.
 *
 * <pre>
 * timetogame.actividad          (key = videojuegoId, delete, 7 dias)
 *   ActividadProducer --VideojuegoVistoEvent--> [3 particiones x 3 replicas]
 *       group "tendencias" (3 hilos, 1 por particion) -> TendenciasService -> SSE /api/public/stream/tendencias
 *       group "historial"                             -> HistorialService  -> tabla historial_vistas
 *
 * timetogame.catalogo.cambios   (key = videojuegoId, compact: queda el ultimo estado de cada juego)
 *   CatalogoCambioProducer --CatalogoCambioEvent--> [3 particiones x 3 replicas]
 *       group "tendencias"    -> TendenciasService (titulo nuevo / saca del ranking al eliminado)
 *       group "catalogo-live" -> StreamService     -> SSE (aviso en vivo del cambio)
 *
 * Error no recuperable o reintentos agotados -> &lt;topico&gt;.DLT (misma particion)
 * </pre>
 */
public final class TopologiaKafka {

    public static final String ACTIVIDAD = "timetogame.actividad";
    public static final String CATALOGO_CAMBIOS = "timetogame.catalogo.cambios";

    /** Sufijo de los DLT (lo fija KafkaConfig, no el default de Spring Kafka). */
    public static final String SUFIJO_DLT = ".DLT";
    public static final String ACTIVIDAD_DLT = ACTIVIDAD + SUFIJO_DLT;
    public static final String CATALOGO_CAMBIOS_DLT = CATALOGO_CAMBIOS + SUFIJO_DLT;

    /** Mismo grupo en varios hilos/instancias: se reparten las particiones. */
    public static final String GRUPO_TENDENCIAS = "tendencias";
    /** Grupo distinto: recibe TODOS los eventos de actividad, independiente de tendencias. */
    public static final String GRUPO_HISTORIAL = "historial";
    public static final String GRUPO_CATALOGO_LIVE = "catalogo-live";

    private TopologiaKafka() {
    }
}
