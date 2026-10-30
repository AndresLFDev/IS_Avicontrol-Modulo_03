package co.edu.unimagdalena.avicontrol.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Lote de la copia local de Módulo 1 (M3-CU07) con lo que necesita la Liquidación. */
public record LoteLiquidable(
    UUID idLote,
    String nombreLote,
    UUID idGalpon,
    String nombreGalpon,
    int poblacionInicial,
    int poblacionActual,
    long costoTotalCop,
    Instant fechaHoraSync) {

  /** CU03.FR-005: siniestro total es población actual 0, no la ausencia de resultado. */
  public boolean siniestroTotal() {
    return poblacionActual == 0;
  }
}
