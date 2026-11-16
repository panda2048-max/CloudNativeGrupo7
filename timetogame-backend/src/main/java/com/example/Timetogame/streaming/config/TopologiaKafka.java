package com.example.Timetogame.streaming.config;

/**
 * Unico lugar con los nombres de topicos y consumer groups de Kafka del backend.
 *
 * <pre>
 * timetogame.actividad          (key = videojuegoId, delete, 7 dias)
 *   ActividadProducer --ActividadEvent (tipo VISTA | RESENA)--> [3 particiones x 3 replicas]
 *       group "tendencias" (3 hilos, 1 por particion) VISTA  -> TendenciasService   -> SSE "tendencias"
 *       group "historial"                             VISTA  -> HistorialService    -> tabla historial_vistas
 *       group "resenas-live"                          RESENA -> AvisosEnVivoService -> SSE "resena"
 *
 * timetogame.catalogo.cambios   (key = videojuegoId, compact: queda el ultimo estado de cada juego)
 *   CatalogoCambioProducer --CatalogoCambioEvent--> [3 particiones x 3 replicas]
 *       group "tendencias"    -> TendenciasService   (titulo nuevo / saca del ranking al eliminado)
 *       group "catalogo-live" -> AvisosEnVivoService -> SSE "catalogo"
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
    /** Toma solo las resenas del topico de actividad y las avisa en vivo. */
    public static final String GRUPO_RESENAS_LIVE = "resenas-live";

    private TopologiaKafka() {
    }
}
