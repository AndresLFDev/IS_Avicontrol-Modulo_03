package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Lote tal como lo informa Módulo 1 (acuerdo de contratos de M1). Los campos pueden venir ausentes
 * (M1 los omite y los reporta en {@code incidencias}), por eso los números son {@link Long}: la
 * validación (CU07.FR-010) decide si el lote se aplica o se omite.
 */
public record LoteM1(
    UUID id,
    String nombre,
    LocalDate fechaIngreso,
    Long poblacionInicial,
    Long poblacionActual,
    Long costoTotal) {}
