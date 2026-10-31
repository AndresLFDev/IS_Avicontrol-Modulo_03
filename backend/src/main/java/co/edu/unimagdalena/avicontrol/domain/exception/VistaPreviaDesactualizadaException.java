package co.edu.unimagdalena.avicontrol.domain.exception;

/** {@code 409 vista-previa-desactualizada}: la huella de la confirmación ya no coincide. */
public class VistaPreviaDesactualizadaException extends RuntimeException {

  public VistaPreviaDesactualizadaException() {
    super(
        "Los datos sincronizados del lote cambiaron después de la vista previa. Revise la nueva"
            + " vista previa antes de confirmar.");
  }
}
