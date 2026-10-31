package co.edu.unimagdalena.avicontrol.domain.exception;

import co.edu.unimagdalena.avicontrol.domain.model.CausaNoLiquidable;
import co.edu.unimagdalena.avicontrol.domain.model.PartidaPendiente;
import java.util.List;

/** {@code 422 lote-no-liquidable}: el lote no cumple una precondición de CU03. */
public class LoteNoLiquidableException extends RuntimeException {

  private final CausaNoLiquidable causa;
  private final List<PartidaPendiente> partidasPendientes;

  public LoteNoLiquidableException(CausaNoLiquidable causa) {
    this(causa, List.of());
  }

  public LoteNoLiquidableException(
      CausaNoLiquidable causa, List<PartidaPendiente> partidasPendientes) {
    super(causa.mensaje());
    this.causa = causa;
    this.partidasPendientes = List.copyOf(partidasPendientes);
  }

  public CausaNoLiquidable getCausa() {
    return causa;
  }

  public List<PartidaPendiente> getPartidasPendientes() {
    return partidasPendientes;
  }
}
