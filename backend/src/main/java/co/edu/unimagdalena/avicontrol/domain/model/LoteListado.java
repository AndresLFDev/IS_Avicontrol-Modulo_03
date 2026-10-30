package co.edu.unimagdalena.avicontrol.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila de la lista de lotes con su etapa ya derivada (M3-CU01). Es una proyección de lectura sobre
 * la copia local, no una entidad.
 */
public record LoteListado(
    UUID idLote,
    String nombreLote,
    LocalDate fechaIngreso,
    UUID idGalpon,
    String nombreGalpon,
    EtapaLote etapa,
    int poblacionActual,
    boolean tieneResultadoSacrificio) {}
