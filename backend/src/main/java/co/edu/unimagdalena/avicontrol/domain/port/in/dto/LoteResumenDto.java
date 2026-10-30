package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila de la lista de lotes (M3-CU01). No incluye mortalidad: se presenta en la Liquidación.
 *
 * @param etapa código de {@code EtapaLote} listable
 * @param siniestroTotal {@code true} cuando la población actual sincronizada es 0
 * @param accion siempre {@code GENERAR_LIQUIDACION} (FR-005)
 * @param accionHabilitada solo en {@code POR_LIQUIDAR} con resultado de sacrificio o siniestro
 *     total
 */
public record LoteResumenDto(
    UUID idLote,
    String nombreLote,
    LocalDate fechaIngreso,
    UUID idGalpon,
    String nombreGalpon,
    String etapa,
    boolean siniestroTotal,
    String accion,
    boolean accionHabilitada) {}
