package co.edu.unimagdalena.avicontrol.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Plan 003, T005: fórmulas de CU03.FR-001 a FR-004 y reglas de redondeo RT-01 y RT-02. */
class CalculoLiquidacionTest {

  @Test
  void casoDoradoDeCu03() {
    var calculo = CalculoLiquidacion.de(DatosPrueba.casoDorado(), new BigDecimal("4500"));

    assertThat(calculo.ventaBrutaCop()).isEqualTo(107_100_000);
    assertThat(calculo.mortalidadAves()).isEqualTo(500);
    assertThat(calculo.porcentajeMortalidad()).isEqualByComparingTo("5.56");
    assertThat(calculo.subtotales())
        .isEqualTo(new SubtotalesCategoria(64_800_000, 2_200_000, 18_000_000));
    assertThat(calculo.costosOperativosCop()).isEqualTo(85_000_000);
    assertThat(calculo.utilidadNetaCop()).isEqualTo(22_100_000);
    assertThat(calculo.pollosVendidos()).isEqualTo(8500);
    assertThat(calculo.pesoTotalKg()).isEqualByComparingTo("23800");
    assertThat(calculo.pesoPromedioKg()).isEqualByComparingTo("2.80");
  }

  @Test
  void ejemploDelPlan003() {
    var lote =
        new LoteLiquidable(
            UUID.randomUUID(), "L", UUID.randomUUID(), "G", 10_000, 9_600, 12_000_000, null);
    var resultado =
        new ResultadoSacrificio(UUID.randomUUID(), 9600, new BigDecimal("24000.00"), null);
    var datos =
        new DatosLiquidacion(
            lote,
            resultado,
            List.of(DatosPrueba.alimento("01", "Engorde", "18000", "2500")),
            List.of(DatosPrueba.tramo("01", "Vacuna", "REC-1", "160", "20000")));

    var calculo = CalculoLiquidacion.de(datos, new BigDecimal("5800.00"));

    assertThat(calculo.ventaBrutaCop()).isEqualTo(139_200_000);
    assertThat(calculo.porcentajeMortalidad()).isEqualByComparingTo("4.00");
    assertThat(calculo.costosOperativosCop()).isEqualTo(60_200_000);
    assertThat(calculo.utilidadNetaCop()).isEqualTo(79_000_000);
    assertThat(calculo.pesoPromedioKg()).isEqualByComparingTo("2.50");
  }

  @Test
  void siniestroTotalNoTieneVentaYLaUtilidadEsNegativa() {
    var lote =
        new LoteLiquidable(
            UUID.randomUUID(), "L", UUID.randomUUID(), "G", 8000, 0, 10_000_000, null);
    var datos =
        new DatosLiquidacion(
            lote,
            null,
            List.of(DatosPrueba.alimento("41", "Inicio", "7000", "3000")),
            List.of(DatosPrueba.tramo("41", "Enrofloxacina", "REC-2026-095", "90", "20000")));

    var calculo = CalculoLiquidacion.de(datos, null);

    assertThat(calculo.ventaBrutaCop()).isZero();
    assertThat(calculo.pollosVendidos()).isZero();
    assertThat(calculo.pesoTotalKg()).isEqualByComparingTo("0");
    assertThat(calculo.pesoPromedioKg()).isNull();
    assertThat(calculo.precioKgCop()).isNull();
    assertThat(calculo.mortalidadAves()).isEqualTo(8000);
    assertThat(calculo.porcentajeMortalidad()).isEqualByComparingTo("100.00");
    assertThat(calculo.costosOperativosCop()).isEqualTo(32_800_000);
    assertThat(calculo.utilidadNetaCop()).isEqualTo(-32_800_000);
  }

  @Test
  void laVentaBrutaRedondeaHalfUpSobreElPesoTotal() {
    var resultado = new ResultadoSacrificio(UUID.randomUUID(), 1, new BigDecimal("1.50"), null);
    var datos =
        new DatosLiquidacion(
            DatosPrueba.lote(1), resultado, DatosPrueba.alimento(), DatosPrueba.medicamentos());

    // 1,50 kg × $3 = $4,50 → $5 (HALF_EVEN daría $4)
    assertThat(CalculoLiquidacion.de(datos, new BigDecimal("3")).ventaBrutaCop()).isEqualTo(5);
  }

  @Test
  void elCostoDePoblacionEsUnaSolaPartidaConElValorDeModulo1() {
    var partidas = DatosPrueba.casoDorado().partidasCosto();

    assertThat(partidas)
        .filteredOn(p -> p.categoria() == CategoriaCosto.POBLACION)
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.subtotalCop()).isEqualTo(18_000_000);
              assertThat(p.concepto()).isEqualTo("Costo de adquisición del lote (9000 aves)");
              assertThat(p.fuenteOrigen()).isEqualTo(FuenteSincronizacion.MODULO_1);
            });
    assertThat(partidas).hasSize(7); // 3 de alimento, 3 tramos de medicamento y 1 de población
  }

  @Test
  void laHuellaEsEstableYCambiaConLosDatosOElPrecio() {
    var datos = DatosPrueba.casoDorado();
    var huella = datos.huella(new BigDecimal("4500"));

    assertThat(datos.huella(new BigDecimal("4500.00"))).isEqualTo(huella);
    assertThat(datos.huella(new BigDecimal("4600"))).isNotEqualTo(huella);
    var resincronizado =
        new DatosLiquidacion(
            DatosPrueba.lote(8499),
            DatosPrueba.resultado(),
            DatosPrueba.alimento(),
            DatosPrueba.medicamentos());
    assertThat(resincronizado.huella(new BigDecimal("4500"))).isNotEqualTo(huella);
  }

  @Test
  void laFechaDeModulo2EsLaMasRecienteDeSusDatos() {
    var tarde = Instant.parse("2026-10-21T00:00:00Z");
    var resultado = new ResultadoSacrificio(UUID.randomUUID(), 1, BigDecimal.ONE, tarde);
    var datos =
        new DatosLiquidacion(
            DatosPrueba.lote(1), resultado, DatosPrueba.alimento(), DatosPrueba.medicamentos());

    assertThat(datos.fechaSincronizacionM2()).isEqualTo(tarde);
    assertThat(datos.fechaSincronizacionM1()).isEqualTo(DatosPrueba.SYNC_M1);
  }
}
