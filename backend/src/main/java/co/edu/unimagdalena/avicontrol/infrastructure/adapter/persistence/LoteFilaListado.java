package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.time.LocalDate;
import java.util.UUID;

/** Fila de la lista de lotes con la etapa ya derivada por la consulta. */
record LoteFilaListado(
    UUID idLote,
    String nombreLote,
    LocalDate fechaIngreso,
    UUID idGalpon,
    String nombreGalpon,
    String etapa,
    int poblacionActual,
    boolean tieneResultado) {}
