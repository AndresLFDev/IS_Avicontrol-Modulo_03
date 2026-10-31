package co.edu.unimagdalena.avicontrol.domain.exception;

import java.util.UUID;

/** {@code 404 lote-no-encontrado}: el lote no existe en la copia local. */
public class LoteNoEncontradoException extends RuntimeException {

  public LoteNoEncontradoException(UUID idLote) {
    super("No se encontró el lote " + idLote + " en la copia local sincronizada.");
  }
}
