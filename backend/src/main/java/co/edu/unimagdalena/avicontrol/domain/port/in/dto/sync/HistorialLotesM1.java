package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.util.List;
import java.util.UUID;

/**
 * Historial de lotes de un galpón en Módulo 1: el lote activo (si hay) y los anteriores, ya
 * desvinculados por un vaciado sanitario aceptado.
 */
public record HistorialLotesM1(UUID galponId, LoteM1 loteActivo, List<LoteM1> lotesAnteriores) {

  public HistorialLotesM1 {
    lotesAnteriores = lotesAnteriores == null ? List.of() : List.copyOf(lotesAnteriores);
  }

  /** Historial de un galpón que M1 ya no conoce. */
  public static HistorialLotesM1 vacio(UUID galponId) {
    return new HistorialLotesM1(galponId, null, List.of());
  }
}
