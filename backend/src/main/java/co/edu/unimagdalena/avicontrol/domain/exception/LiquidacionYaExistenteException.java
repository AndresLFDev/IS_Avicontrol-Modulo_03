package co.edu.unimagdalena.avicontrol.domain.exception;

import java.util.UUID;

/**
 * {@code 409 liquidacion-ya-existente} (CU03.FR-007). {@code idLiquidacionActiva} es nulo cuando la
 * detecta el índice {@code uq_lote_activa} en una confirmación concurrente.
 */
public class LiquidacionYaExistenteException extends RuntimeException {

  private final Long idLiquidacionActiva;

  public LiquidacionYaExistenteException(UUID idLote, Long idLiquidacionActiva) {
    super(
        "El lote "
            + idLote
            + " ya tiene una Liquidación ACTIVA"
            + (idLiquidacionActiva == null ? "" : " (ID: " + idLiquidacionActiva + ")")
            + ". Anúlela antes de generar una nueva.");
    this.idLiquidacionActiva = idLiquidacionActiva;
  }

  public Long getIdLiquidacionActiva() {
    return idLiquidacionActiva;
  }
}
