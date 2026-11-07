package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila del historial de Liquidaciones (M3-CU06.FR-002). {@code idLiquidacion} enlaza con la vista
 * de la Liquidación, no con su desglose (FR-006).
 *
 * @param estado {@code ACTIVA} o {@code ANULADA}
 */
public record HistorialItemDto(
    long idLiquidacion,
    Instant fechaHoraGeneracion,
    UUID idGalpon,
    String nombreGalpon,
    UUID idLote,
    String nombreLote,
    long ventaBrutaCop,
    BigDecimal porcentajeMortalidad,
    long costosOperativosCop,
    long utilidadNetaCop,
    String estado) {}
