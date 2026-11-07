package co.edu.unimagdalena.avicontrol.domain.exception;

import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;

/** {@code 409 liquidacion-ya-anulada} con el registro existente (CU04 esc. 4). */
public class LiquidacionYaAnuladaException extends RuntimeException {

  private final transient RegistroAnulacion registroAnulacion;

  public LiquidacionYaAnuladaException(long idLiquidacion, RegistroAnulacion registroAnulacion) {
    super("La Liquidación " + idLiquidacion + " ya fue anulada.");
    this.registroAnulacion = registroAnulacion;
  }

  public RegistroAnulacion getRegistroAnulacion() {
    return registroAnulacion;
  }
}
