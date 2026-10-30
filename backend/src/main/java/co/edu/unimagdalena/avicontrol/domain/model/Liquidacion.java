package co.edu.unimagdalena.avicontrol.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Liquidación persistida con sus valores congelados (snapshot financiero). Se lee tal como se
 * emitió; nunca se recalcula (CU03.FR-014).
 */
public record Liquidacion(
    long idLiquidacion,
    EstadoLiquidacion estado,
    UUID idLote,
    UUID idGalpon,
    String nombreLote,
    String nombreGalpon,
    UUID idResultadoSacrificio,
    int pollosVendidos,
    BigDecimal pesoTotalKg,
    BigDecimal pesoPromedioKg,
    BigDecimal precioKgCop,
    long ventaBrutaCop,
    int mortalidadAves,
    BigDecimal porcentajeMortalidad,
    SubtotalesCategoria subtotales,
    long costosOperativosCop,
    long utilidadNetaCop,
    Instant fechaHoraGeneracion,
    String usuarioResponsable,
    Instant fechaSincronizacionM1,
    Instant fechaSincronizacionM2,
    RegistroAnulacion registroAnulacion) {

  /** Solo el siniestro total se liquida sin resultado de sacrificio. */
  public boolean siniestroTotal() {
    return idResultadoSacrificio == null;
  }
}
