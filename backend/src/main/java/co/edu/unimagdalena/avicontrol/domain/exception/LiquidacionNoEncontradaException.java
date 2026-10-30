package co.edu.unimagdalena.avicontrol.domain.exception;

/** {@code 404 liquidacion-no-encontrada}. */
public class LiquidacionNoEncontradaException extends RuntimeException {

  public LiquidacionNoEncontradaException(long idLiquidacion) {
    super("No se encontró la liquidación con ID " + idLiquidacion + ".");
  }
}
