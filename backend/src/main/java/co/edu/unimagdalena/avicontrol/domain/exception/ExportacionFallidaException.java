package co.edu.unimagdalena.avicontrol.domain.exception;

/** La generación del Excel falló; termina en {@code 500 error-interno} (CU05.FR-005). */
public class ExportacionFallidaException extends RuntimeException {

  public ExportacionFallidaException(long idLiquidacion, Throwable causa) {
    super("No se pudo generar el Excel de la Liquidación " + idLiquidacion + ".", causa);
  }
}
