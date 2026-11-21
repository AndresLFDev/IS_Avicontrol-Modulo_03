package co.edu.unimagdalena.avicontrol.application.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteConsultableM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.PartidaAlimentoM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.PartidaMedicamentoM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResultadoM2;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM2Repository;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM2Repository.ResultadoLocal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CU08 a CU10: validación y escritura de los datos de un lote que entrega M2. */
class AplicarLoteM2ServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-21T10:00:00Z");
  private static final UUID LOTE = UUID.fromString("7331eec8-e447-4868-bdef-8eca3fff8d44");
  private static final UUID GALPON = UUID.fromString("a402a368-cd4d-4e27-8d48-9e0d7eb8d374");
  private static final UUID RESULTADO = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final UUID SUMINISTRO = UUID.fromString("5a000000-0000-4000-8000-000000000001");
  private static final UUID RECEPCION_1 = UUID.fromString("6b000000-0000-4000-8000-000000000001");
  private static final UUID RECEPCION_2 = UUID.fromString("6b000000-0000-4000-8000-000000000002");
  private static final UUID CONSUMO = UUID.fromString("7c000000-0000-4000-8000-000000000001");
  private static final LoteConsultableM2 LOTE_LOCAL = new LoteConsultableM2(LOTE, GALPON);

  private CopiaLocalM2Repository copia;
  private AplicarLoteM2Service servicio;

  @BeforeEach
  void preparar() {
    copia = mock(CopiaLocalM2Repository.class);
    when(copia.resultadoDelLote(LOTE)).thenReturn(Optional.empty());
    servicio = new AplicarLoteM2Service(copia);
  }

  private AplicarLoteM2Service.Aplicacion aplicar(
      Optional<ResultadoM2> resultado,
      List<PartidaAlimentoM2> alimentos,
      List<PartidaMedicamentoM2> medicamentos) {
    return servicio.aplicar(LOTE_LOCAL, resultado, alimentos, medicamentos, AHORA);
  }

  private static ResultadoM2 resultado(UUID lote, Integer pollos, String peso) {
    return new ResultadoM2(
        RESULTADO,
        lote,
        GALPON,
        pollos,
        peso == null ? null : new BigDecimal(peso),
        Instant.parse("2026-11-20T18:30:00Z"));
  }

  private static PartidaAlimentoM2 alimento(UUID recepcion, String kg, String precio) {
    return new PartidaAlimentoM2(
        SUMINISTRO,
        recepcion,
        "Inicio Premium",
        "INICIO",
        new BigDecimal(kg),
        precio == null ? null : new BigDecimal(precio),
        new BigDecimal("3.80"));
  }

  private static PartidaMedicamentoM2 tramo(UUID recepcion, String cantidad, String precio) {
    return new PartidaMedicamentoM2(
        CONSUMO,
        recepcion,
        UUID.fromString("8d000000-0000-4000-8000-000000000001"),
        "Enrofloxacina",
        LocalDate.of(2026, 11, 12),
        new BigDecimal(cantidad),
        "ML",
        precio == null ? null : new BigDecimal(precio),
        new BigDecimal("0.76"));
  }

  // --- Resultado ---

  @Test
  void guardaElResultadoConElGalponDelLoteLocal() {
    var aplicado = aplicar(Optional.of(resultado(LOTE, 1974, "4012.50")), List.of(), List.of());

    assertThat(aplicado.registros()).isEqualTo(1);
    assertThat(aplicado.incidencias()).isEmpty();
    verify(copia)
        .guardarResultado(
            RESULTADO,
            LOTE,
            GALPON,
            1974,
            new BigDecimal("4012.50"),
            Instant.parse("2026-11-20T18:30:00Z"),
            AHORA);
  }

  @Test
  void sinResultadoEnM2NoEscribeNiInforma() {
    var aplicado = aplicar(Optional.empty(), List.of(), List.of());

    assertThat(aplicado.registros()).isZero();
    assertThat(aplicado.incidencias()).isEmpty();
    verify(copia, never()).guardarResultado(any(), any(), any(), eq(0), any(), any(), any());
  }

  @Test
  void unResultadoYaUtilizadoNoSeToca() {
    when(copia.resultadoDelLote(LOTE)).thenReturn(Optional.of(new ResultadoLocal(RESULTADO, true)));

    var aplicado = aplicar(Optional.of(resultado(LOTE, 1500, "3000")), List.of(), List.of());

    assertThat(aplicado.registros()).isZero();
    verify(copia, never()).guardarResultado(any(), any(), any(), eq(1500), any(), any(), any());
  }

  @Test
  void unResultadoSinUtilizarSiSeActualiza() {
    when(copia.resultadoDelLote(LOTE))
        .thenReturn(Optional.of(new ResultadoLocal(RESULTADO, false)));

    var aplicado = aplicar(Optional.of(resultado(LOTE, 1950, "3990")), List.of(), List.of());

    assertThat(aplicado.registros()).isEqualTo(1);
  }

  @Test
  void omiteUnResultadoInvalidoYLoInforma() {
    for (var invalido :
        List.of(
            resultado(UUID.randomUUID(), 1974, "4012.50"), // de otro lote
            resultado(LOTE, 0, "4012.50"),
            resultado(LOTE, null, "4012.50"),
            resultado(LOTE, 1974, "0"),
            resultado(LOTE, 1974, null))) {
      var aplicado = aplicar(Optional.of(invalido), List.of(), List.of());

      assertThat(aplicado.registros()).isZero();
      assertThat(aplicado.incidencias()).hasSize(1);
      assertThat(aplicado.incidencias().get(0)).contains("Resultado de sacrificio");
    }
    verify(copia, never()).guardarResultado(any(), any(), any(), eq(1974), any(), any(), any());
  }

  // --- Alimento ---

  @Test
  void guardaCadaPartidaDeAlimentoConUnIdDeterministaPorSuministroYRecepcion() {
    var aplicado =
        aplicar(
            Optional.empty(),
            List.of(alimento(RECEPCION_1, "2", "10.0000"), alimento(RECEPCION_2, "1.5", "11.25")),
            List.of());

    assertThat(aplicado.registros()).isEqualTo(2);
    UUID id1 = AplicarLoteM2Service.idPartidaAlimento(SUMINISTRO, RECEPCION_1);
    UUID id2 = AplicarLoteM2Service.idPartidaAlimento(SUMINISTRO, RECEPCION_2);
    assertThat(id1).isNotEqualTo(id2); // mismo suministro, recepciones distintas
    assertThat(id1).isEqualTo(AplicarLoteM2Service.idPartidaAlimento(SUMINISTRO, RECEPCION_1));
    verify(copia)
        .guardarPartidaAlimento(
            eq(id1),
            eq(LOTE),
            eq(GALPON),
            eq("INICIO"),
            eq(new BigDecimal("2")),
            eq(new BigDecimal("10.0000")),
            eq(new BigDecimal("3.80")),
            eq("recepción " + RECEPCION_1 + " Inicio Premium"),
            eq(AHORA));
    verify(copia, times(2))
        .guardarPartidaAlimento(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void unaPartidaSinPrecioSeGuardaConPrecioNuloParaQueQuedePendiente() {
    aplicar(Optional.empty(), List.of(alimento(RECEPCION_1, "2", null)), List.of());

    verify(copia)
        .guardarPartidaAlimento(any(), any(), any(), any(), any(), eq(null), any(), any(), any());
  }

  @Test
  void omiteLasPartidasDeAlimentoInvalidasSinImpedirLasValidas() {
    var sinRecepcion =
        new PartidaAlimentoM2(SUMINISTRO, null, "x", "INICIO", BigDecimal.ONE, null, null);
    var cantidadCero = alimento(RECEPCION_2, "0", "10");
    var sinTipo =
        new PartidaAlimentoM2(
            SUMINISTRO, UUID.randomUUID(), " ", null, BigDecimal.ONE, BigDecimal.TEN, null);
    var precioNegativo = alimento(UUID.randomUUID(), "1", "-5");

    var aplicado =
        aplicar(
            Optional.empty(),
            List.of(
                sinRecepcion,
                cantidadCero,
                sinTipo,
                precioNegativo,
                alimento(RECEPCION_1, "2", "10")),
            List.of());

    assertThat(aplicado.registros()).isEqualTo(1);
    assertThat(aplicado.incidencias()).hasSize(4);
  }

  @Test
  void unaPartidaRepetidaEnLaRespuestaSeOmite() {
    var aplicado =
        aplicar(
            Optional.empty(),
            List.of(alimento(RECEPCION_1, "2", "10"), alimento(RECEPCION_1, "2", "10")),
            List.of());

    assertThat(aplicado.registros()).isEqualTo(1);
    assertThat(aplicado.incidencias()).hasSize(1);
  }

  @Test
  void sinTipoUsaElNombreDelAlimentoYRecortaLoQueNoCabe() {
    var largo = "A".repeat(80);
    var partida =
        new PartidaAlimentoM2(
            SUMINISTRO, RECEPCION_1, largo, null, BigDecimal.ONE, BigDecimal.TEN, null);

    aplicar(Optional.empty(), List.of(partida), List.of());

    verify(copia)
        .guardarPartidaAlimento(
            any(), any(), any(), eq("A".repeat(50)), any(), any(), eq(null), any(), any());
  }

  // --- Medicamento ---

  @Test
  void agrupaLosTramosDeUnConsumoYSumaSuCantidadSinPromediarPrecios() {
    var aplicado =
        aplicar(
            Optional.empty(),
            List.of(),
            List.of(tramo(RECEPCION_1, "100", "0.0333"), tramo(RECEPCION_2, "20.5", "0.0400")));

    assertThat(aplicado.registros()).isEqualTo(3); // 1 consumo + 2 tramos
    verify(copia)
        .guardarConsumoMedicamento(
            CONSUMO,
            LOTE,
            GALPON,
            "Enrofloxacina",
            LocalDate.of(2026, 11, 12),
            new BigDecimal("120.5"),
            "ML",
            AHORA);
    verify(copia)
        .guardarTramoMedicamento(
            CONSUMO,
            RECEPCION_1.toString(),
            new BigDecimal("100"),
            new BigDecimal("0.0333"),
            new BigDecimal("0.76"));
    verify(copia)
        .guardarTramoMedicamento(
            CONSUMO,
            RECEPCION_2.toString(),
            new BigDecimal("20.5"),
            new BigDecimal("0.0400"),
            new BigDecimal("0.76"));
  }

  @Test
  void unConsumoConUnTramoInvalidoSeOmiteCompletoParaNoCobrarDeMenos() {
    var aplicado =
        aplicar(
            Optional.empty(),
            List.of(),
            List.of(tramo(RECEPCION_1, "100", "0.0333"), tramo(RECEPCION_2, "0", "0.04")));

    assertThat(aplicado.registros()).isZero();
    assertThat(aplicado.incidencias()).hasSize(1);
    assertThat(aplicado.incidencias().get(0)).contains("Consumo de medicamento " + CONSUMO);
    verify(copia, never())
        .guardarConsumoMedicamento(any(), any(), any(), any(), any(), any(), any(), any());
    verify(copia, never()).guardarTramoMedicamento(any(), any(), any(), any(), any());
  }

  @Test
  void omiteElConsumoSiSusTramosTienenUnidadesDistintasOLaMismaRecepcionDosVeces() {
    var otraUnidad =
        new PartidaMedicamentoM2(
            CONSUMO,
            RECEPCION_2,
            null,
            "Enrofloxacina",
            LocalDate.of(2026, 11, 12),
            BigDecimal.TEN,
            "GR",
            BigDecimal.ONE,
            null);

    var unidades =
        aplicar(Optional.empty(), List.of(), List.of(tramo(RECEPCION_1, "5", "1"), otraUnidad));
    var repetida =
        aplicar(
            Optional.empty(),
            List.of(),
            List.of(tramo(RECEPCION_1, "5", "1"), tramo(RECEPCION_1, "5", "1")));

    assertThat(unidades.registros()).isZero();
    assertThat(unidades.incidencias().get(0)).contains("unidades base distintas");
    assertThat(repetida.registros()).isZero();
    assertThat(repetida.incidencias().get(0)).contains("repetida");
  }

  @Test
  void sinNombreDeMedicamentoUsaSuIdEnVezDeInventarUnNombre() {
    var sinNombre =
        new PartidaMedicamentoM2(
            CONSUMO,
            RECEPCION_1,
            UUID.fromString("8d000000-0000-4000-8000-000000000001"),
            null,
            LocalDate.of(2026, 11, 12),
            BigDecimal.TEN,
            "ML",
            BigDecimal.ONE,
            null);

    aplicar(Optional.empty(), List.of(), List.of(sinNombre));

    verify(copia)
        .guardarConsumoMedicamento(
            any(),
            any(),
            any(),
            eq("Medicamento 8d000000-0000-4000-8000-000000000001"),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void unTramoSinConsumoIdSeOmiteYSeInforma() {
    var sinConsumo =
        new PartidaMedicamentoM2(
            null,
            RECEPCION_1,
            null,
            "x",
            LocalDate.of(2026, 11, 12),
            BigDecimal.TEN,
            "ML",
            BigDecimal.ONE,
            null);

    var aplicado = aplicar(Optional.empty(), List.of(), List.of(sinConsumo));

    assertThat(aplicado.registros()).isZero();
    assertThat(aplicado.incidencias()).hasSize(1);
  }
}
