package co.edu.unimagdalena.avicontrol.domain.exception;

/** {@code 409 exportacion-no-permitida}: el Excel de una Liquidación ANULADA (CU05.FR-004). */
public class ExportacionNoPermitidaException extends RuntimeException {

  public ExportacionNoPermitidaException(long idLiquidacion) {
    super(
        "La Liquidación "
            + idLiquidacion
            + " está anulada: su desglose es de solo lectura y no se puede exportar.");
  }
}
