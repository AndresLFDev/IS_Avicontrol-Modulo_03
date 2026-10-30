package co.edu.unimagdalena.avicontrol.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Datos de la Liquidación para las pruebas: los mismos del Caso Dorado de CU03 y del seed. */
public final class DatosPrueba {

  public static final UUID ID_LOTE = UUID.fromString("b0000000-0000-0000-0000-000000000003");
  public static final UUID ID_GALPON = UUID.fromString("a0000000-0000-0000-0000-000000000003");
  public static final UUID ID_RESULTADO = UUID.fromString("d0000000-0000-0000-0000-000000000003");
  public static final Instant SYNC_M1 = Instant.parse("2026-10-20T13:00:01Z");
  public static final Instant SYNC_M2 = Instant.parse("2026-10-20T14:20:01Z");

  private DatosPrueba() {}

  /** 9.000 aves iniciales, 8.500 actuales y costo de población de $18.000.000. */
  public static LoteLiquidable lote(int poblacionActual) {
    return new LoteLiquidable(
        ID_LOTE, "L-2026-03", ID_GALPON, "Galpón Este", 9000, poblacionActual, 18_000_000, SYNC_M1);
  }

  /** 8.500 pollos y 23.800 kg. */
  public static ResultadoSacrificio resultado() {
    return new ResultadoSacrificio(ID_RESULTADO, 8500, new BigDecimal("23800.00"), SYNC_M2);
  }

  /** $15.600.000 + $30.000.000 + $19.200.000 = $64.800.000. */
  public static List<PartidaAlimento> alimento() {
    return List.of(
        alimento("31", "Pre-inicio", "6000.000", "2600.00"),
        alimento("32", "Inicio", "12000.000", "2500.00"),
        alimento("33", "Engorde", "8000.000", "2400.00"));
  }

  /** $600.000 + $420.000 (dos recepciones, sin promediar) + $1.180.000 = $2.200.000. */
  public static List<TramoMedicamento> medicamentos() {
    return List.of(
        tramo("31", "Amoxicilina", "REC-2026-090", "30.000", "20000.00"),
        tramo("31", "Amoxicilina", "REC-2026-101", "20.000", "21000.00"),
        tramo("32", "Vacuna Newcastle", "REC-2026-085", "9440.000", "125.00"));
  }

  /** Caso Dorado: costos operativos de $85.000.000. */
  public static DatosLiquidacion casoDorado() {
    return new DatosLiquidacion(lote(8500), resultado(), alimento(), medicamentos());
  }

  public static PartidaAlimento alimento(
      String sufijo, String tipo, String cantidadKg, String precio) {
    return new PartidaAlimento(
        UUID.fromString("f0000000-0000-0000-0000-0000000000" + sufijo),
        tipo,
        new BigDecimal(cantidadKg),
        precio == null ? null : new BigDecimal(precio),
        precio == null,
        "REC-" + sufijo,
        SYNC_M2);
  }

  public static TramoMedicamento tramo(
      String sufijo, String medicamento, String recepcion, String cantidad, String precio) {
    return new TramoMedicamento(
        UUID.fromString("90000000-0000-0000-0000-0000000000" + sufijo),
        medicamento,
        recepcion,
        new BigDecimal(cantidad),
        "ML",
        precio == null ? null : new BigDecimal(precio),
        precio == null,
        SYNC_M2);
  }
}
