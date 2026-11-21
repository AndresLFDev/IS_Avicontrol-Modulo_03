package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tramo de un consumo de medicamento, separado por recepción (M3-CU10). Se identifica por el par
 * {@code (consumoId, recepcionId)}; M3 agrupa los tramos por consumo sin promediar sus precios.
 */
public record PartidaMedicamentoM2(
    UUID consumoId,
    UUID recepcionId,
    UUID medicamentoId,
    String nombreMedicamento,
    LocalDate fechaAplicacion,
    BigDecimal cantidadUnidadBase,
    String unidadBase,
    BigDecimal precioNetoUnidadBase,
    BigDecimal valorImpuestoCop) {}
