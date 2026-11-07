package co.edu.unimagdalena.avicontrol.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.model.CategoriaCosto;
import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.model.Liquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.PartidaCosto;
import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;
import co.edu.unimagdalena.avicontrol.domain.model.SubtotalesCategoria;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.CategoriaDesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Plan 004, T006: el desglose se arma con las partidas congeladas y cuadra al peso. */
class ConsultarDesgloseServiceTest {

  private static final Instant FECHA = Instant.parse("2026-10-30T12:00:00Z");
  private static final UUID LOTE = UUID.fromString("b0000000-0000-0000-0000-000000000003");

  private final LiquidacionRepository repositorio = mock(LiquidacionRepository.class);
  private final ConsultarDesgloseService servicio = new ConsultarDesgloseService(repositorio);

  @Test
  void agrupaPorCategoriasYCuadraConLosCostosOperativos() {
    when(repositorio.buscarPorId(42L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ACTIVA, null, resultado())));
    when(repositorio.buscarPartidas(42L))
        .thenReturn(
            List.of(
                partida(CategoriaCosto.ALIMENTO, "Inicio", 30_000_000, "REC-1"),
                partida(CategoriaCosto.MEDICINA, "Amoxicilina", 2_200_000, "REC-2"),
                partida(CategoriaCosto.ALIMENTO, "Engorde", 34_800_000, "REC-3"),
                partida(CategoriaCosto.POBLACION, "Costo del lote", 18_000_000, null)));

    var dto = servicio.consultarDesglose(42L);

    assertThat(dto.categorias())
        .extracting(CategoriaDesgloseDto::categoria, CategoriaDesgloseDto::etiqueta)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("ALIMENTO", "Alimento"),
            org.assertj.core.groups.Tuple.tuple("MEDICINA", "Insumos Médicos"),
            org.assertj.core.groups.Tuple.tuple("POBLACION", "Costo de Población"));
    assertThat(dto.categorias().getFirst().partidas())
        .extracting(p -> p.concepto())
        .containsExactly("Inicio", "Engorde");
    assertThat(dto.categorias().getFirst().subtotalCop()).isEqualTo(64_800_000);
    assertThat(dto.categorias().stream().mapToLong(CategoriaDesgloseDto::subtotalCop).sum())
        .isEqualTo(dto.costosOperativosCop());
    assertThat(dto.ingresos().ventaBrutaCop()).isEqualTo(107_100_000);
    assertThat(dto.ingresos().pesoPromedioKg()).isEqualByComparingTo("2.80");
    assertThat(dto.mortalidadAves()).isEqualTo(500);
    assertThat(dto.exportacionPermitida()).isTrue();
    assertThat(dto.registroAnulacion()).isNull();
  }

  @Test
  void elCostoDePoblacionSeRastreaHastaElLote() {
    when(repositorio.buscarPorId(42L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ACTIVA, null, resultado())));
    when(repositorio.buscarPartidas(42L))
        .thenReturn(List.of(partida(CategoriaCosto.POBLACION, "Costo", 18_000_000, null)));

    var fila = servicio.consultarDesglose(42L).categorias().getLast().partidas().getFirst();

    assertThat(fila.referenciaOrigen()).isEqualTo(LOTE.toString());
    assertThat(fila.fuenteOrigen()).isEqualTo("MODULO_1");
  }

  @Test
  void unaCategoriaSinPartidasSeMuestraEnCero() {
    when(repositorio.buscarPorId(42L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ACTIVA, null, resultado())));
    when(repositorio.buscarPartidas(42L)).thenReturn(List.of());

    var dto = servicio.consultarDesglose(42L);

    assertThat(dto.categorias()).hasSize(3);
    assertThat(dto.categorias().get(1).partidas()).isEmpty();
    assertThat(dto.categorias().get(1).subtotalCop()).isZero();
  }

  @Test
  void elSiniestroTotalNoTieneVenta() {
    when(repositorio.buscarPorId(42L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ACTIVA, null, null)));

    var dto = servicio.consultarDesglose(42L);

    assertThat(dto.siniestroTotal()).isTrue();
    assertThat(dto.ingresos().pollosVendidos()).isZero();
    assertThat(dto.ingresos().ventaBrutaCop()).isZero();
    assertThat(dto.ingresos().precioKgCop()).isNull();
    assertThat(dto.ingresos().pesoPromedioKg()).isNull();
    assertThat(dto.exportacionPermitida()).isTrue();
  }

  @Test
  void unaAnuladaTraeSuRegistroYNoSeExporta() {
    var registro = new RegistroAnulacion(7L, "Error en el precio", FECHA, "Ana");
    when(repositorio.buscarPorId(40L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ANULADA, registro, resultado())));

    var dto = servicio.consultarDesglose(40L);

    assertThat(dto.estado()).isEqualTo("ANULADA");
    assertThat(dto.registroAnulacion()).isEqualTo(registro);
    assertThat(dto.exportacionPermitida()).isFalse();
  }

  @Test
  void unaInexistenteResponde404() {
    when(repositorio.buscarPorId(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servicio.consultarDesglose(999L))
        .isInstanceOf(LiquidacionNoEncontradaException.class);
  }

  private static UUID resultado() {
    return UUID.randomUUID();
  }

  private static PartidaCosto partida(
      CategoriaCosto categoria, String concepto, long subtotal, String referencia) {
    return new PartidaCosto(
        categoria,
        concepto,
        BigDecimal.ONE,
        "KG",
        BigDecimal.valueOf(subtotal),
        subtotal,
        categoria == CategoriaCosto.POBLACION
            ? FuenteSincronizacion.MODULO_1
            : FuenteSincronizacion.MODULO_2,
        referencia);
  }

  private static Liquidacion liquidacion(
      EstadoLiquidacion estado, RegistroAnulacion anulacion, UUID idResultado) {
    boolean conVenta = idResultado != null;
    return new Liquidacion(
        42L,
        estado,
        LOTE,
        UUID.randomUUID(),
        "L-2026-03",
        "Galpón Este",
        idResultado,
        conVenta ? 8500 : 0,
        conVenta ? new BigDecimal("23800.00") : new BigDecimal("0.00"),
        conVenta ? new BigDecimal("2.80") : null,
        conVenta ? new BigDecimal("4500.00") : null,
        conVenta ? 107_100_000 : 0,
        500,
        new BigDecimal("5.56"),
        new SubtotalesCategoria(64_800_000, 2_200_000, 18_000_000),
        85_000_000,
        conVenta ? 22_100_000 : -85_000_000,
        FECHA,
        "Jorge",
        FECHA,
        FECHA,
        anulacion);
  }
}
