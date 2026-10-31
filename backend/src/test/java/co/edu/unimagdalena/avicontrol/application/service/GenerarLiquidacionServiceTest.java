package co.edu.unimagdalena.avicontrol.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaExistenteException;
import co.edu.unimagdalena.avicontrol.domain.exception.LoteNoEncontradoException;
import co.edu.unimagdalena.avicontrol.domain.exception.LoteNoLiquidableException;
import co.edu.unimagdalena.avicontrol.domain.exception.ParametroInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.exception.VistaPreviaDesactualizadaException;
import co.edu.unimagdalena.avicontrol.domain.model.CalculoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.CategoriaCosto;
import co.edu.unimagdalena.avicontrol.domain.model.CausaNoLiquidable;
import co.edu.unimagdalena.avicontrol.domain.model.DatosLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.DatosPrueba;
import co.edu.unimagdalena.avicontrol.domain.model.PartidaPendiente;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.GenerarLiquidacionCommand;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionRepository;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalLiquidacionRepository;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Plan 003, T006 y T008: precondiciones, vista previa sin persistir y confirmación. */
class GenerarLiquidacionServiceTest {

  private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");
  private static final BigDecimal PRECIO = new BigDecimal("4500");
  private static final String USUARIO = "Jorge Meléndez";

  private CopiaLocalLiquidacionRepository copiaLocal;
  private LiquidacionRepository liquidaciones;
  private AvisoUtilizacionRepository avisos;
  private ConsultarLiquidacionUseCase consultar;
  private GenerarLiquidacionService servicio;

  @BeforeEach
  void configurar() {
    copiaLocal = mock(CopiaLocalLiquidacionRepository.class);
    liquidaciones = mock(LiquidacionRepository.class);
    avisos = mock(AvisoUtilizacionRepository.class);
    consultar = mock(ConsultarLiquidacionUseCase.class);
    servicio =
        new GenerarLiquidacionService(
            copiaLocal, liquidaciones, avisos, consultar, Clock.fixed(AHORA, ZoneOffset.UTC));
    datosDelCasoDorado(8500);
  }

  private void datosDelCasoDorado(int poblacionActual) {
    var id = DatosPrueba.ID_LOTE;
    when(copiaLocal.buscarLote(id)).thenReturn(Optional.of(DatosPrueba.lote(poblacionActual)));
    when(liquidaciones.buscarIdActivaPorLote(id)).thenReturn(Optional.empty());
    when(copiaLocal.tieneAlertaVaciado(id)).thenReturn(true);
    when(copiaLocal.buscarResultadoSacrificio(id)).thenReturn(Optional.of(DatosPrueba.resultado()));
    when(copiaLocal.buscarPartidasAlimento(id)).thenReturn(DatosPrueba.alimento());
    when(copiaLocal.buscarTramosMedicamento(id)).thenReturn(DatosPrueba.medicamentos());
  }

  private static GenerarLiquidacionCommand previa(BigDecimal precio) {
    return new GenerarLiquidacionCommand(DatosPrueba.ID_LOTE, precio, USUARIO, null);
  }

  @Test
  void laVistaPreviaCalculaSinPersistir() {
    var previa = servicio.calcularVistaPrevia(previa(PRECIO));

    assertThat(previa.esVistaPrevia()).isTrue();
    assertThat(previa.aviso()).isEqualTo("Vista previa · sin generar");
    assertThat(previa.ventaBrutaCop()).isEqualTo(107_100_000);
    assertThat(previa.costosOperativosCop()).isEqualTo(85_000_000);
    assertThat(previa.utilidadNetaCop()).isEqualTo(22_100_000);
    assertThat(previa.porcentajeMortalidad()).isEqualByComparingTo("5.56");
    assertThat(previa.nombreGalpon()).isEqualTo("Galpón Este");
    assertThat(previa.fechaSincronizacionM1()).isEqualTo(DatosPrueba.SYNC_M1);
    assertThat(previa.fechaSincronizacionM2()).isEqualTo(DatosPrueba.SYNC_M2);
    assertThat(previa.huellaDatos()).hasSize(64);
    verify(liquidaciones, never()).guardarActiva(any(), any(), any(), any());
    verifyNoInteractions(avisos);
  }

  @Test
  void laHuellaEsLaMismaParaLosMismosDatos() {
    assertThat(servicio.calcularVistaPrevia(previa(PRECIO)).huellaDatos())
        .isEqualTo(servicio.calcularVistaPrevia(previa(new BigDecimal("4500.00"))).huellaDatos());
  }

  @Test
  void loteInexistenteResponde404AntesQueElPrecio() {
    when(copiaLocal.buscarLote(DatosPrueba.ID_LOTE)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servicio.calcularVistaPrevia(previa(null)))
        .isInstanceOf(LoteNoEncontradoException.class);
  }

  @Test
  void sinPrecioEnUnLoteConVentaEsInvalido() {
    assertThatThrownBy(() -> servicio.calcularVistaPrevia(previa(null)))
        .isInstanceOf(ParametroInvalidoException.class)
        .extracting("campo")
        .isEqualTo("precioKgCop");
  }

  @Test
  void conPrecioEnSiniestroTotalEsInvalido() {
    datosDelCasoDorado(0);

    assertThatThrownBy(() -> servicio.calcularVistaPrevia(previa(PRECIO)))
        .isInstanceOf(ParametroInvalidoException.class);
  }

  @Test
  void loteConLiquidacionActivaResponde409ConSuId() {
    when(liquidaciones.buscarIdActivaPorLote(DatosPrueba.ID_LOTE)).thenReturn(Optional.of(42L));

    assertThatThrownBy(() -> servicio.calcularVistaPrevia(previa(PRECIO)))
        .isInstanceOf(LiquidacionYaExistenteException.class)
        .extracting("idLiquidacionActiva")
        .isEqualTo(42L);
  }

  @Test
  void sinAlertaDeVaciadoNoEsLiquidable() {
    when(copiaLocal.tieneAlertaVaciado(DatosPrueba.ID_LOTE)).thenReturn(false);

    assertCausa(CausaNoLiquidable.SIN_ALERTA_VACIADO);
  }

  @Test
  void sinResultadoYConAvesVivasNoEsLiquidable() {
    when(copiaLocal.buscarResultadoSacrificio(DatosPrueba.ID_LOTE)).thenReturn(Optional.empty());

    assertCausa(CausaNoLiquidable.SIN_RESULTADO_SACRIFICIO);
  }

  @Test
  void sinPartidasDeAlimentoNoEsLiquidable() {
    when(copiaLocal.buscarPartidasAlimento(DatosPrueba.ID_LOTE)).thenReturn(List.of());

    assertCausa(CausaNoLiquidable.SIN_PARTIDAS_ALIMENTO);
  }

  @Test
  void sinMedicamentosSiEsLiquidable() {
    when(copiaLocal.buscarTramosMedicamento(DatosPrueba.ID_LOTE)).thenReturn(List.of());

    assertThat(servicio.calcularVistaPrevia(previa(PRECIO)).subtotalesCategoria().insumosMedicos())
        .isZero();
  }

  @Test
  void listaLasPartidasSinPrecio() {
    when(copiaLocal.buscarPartidasAlimento(DatosPrueba.ID_LOTE))
        .thenReturn(List.of(DatosPrueba.alimento("62", "Engorde", "9000", null)));
    when(copiaLocal.buscarTramosMedicamento(DatosPrueba.ID_LOTE))
        .thenReturn(List.of(DatosPrueba.tramo("31", "Amoxicilina", "REC-9", "1", null)));

    assertThatThrownBy(() -> servicio.calcularVistaPrevia(previa(PRECIO)))
        .isInstanceOfSatisfying(
            LoteNoLiquidableException.class,
            e -> {
              assertThat(e.getCausa()).isEqualTo(CausaNoLiquidable.PARTIDAS_SIN_PRECIO);
              assertThat(e.getPartidasPendientes())
                  .containsExactly(
                      new PartidaPendiente(CategoriaCosto.ALIMENTO, "Engorde", "REC-62"),
                      new PartidaPendiente(CategoriaCosto.MEDICINA, "Amoxicilina", "REC-9"));
            });
  }

  @Test
  void siniestroTotalSinResultadoNiPrecioEsLiquidable() {
    datosDelCasoDorado(0);
    when(copiaLocal.buscarResultadoSacrificio(DatosPrueba.ID_LOTE)).thenReturn(Optional.empty());

    var previa = servicio.calcularVistaPrevia(previa(null));

    assertThat(previa.siniestroTotal()).isTrue();
    assertThat(previa.ventaBrutaCop()).isZero();
    assertThat(previa.mortalidadAves()).isEqualTo(9000);
    assertThat(previa.utilidadNetaCop()).isEqualTo(-85_000_000);
  }

  @Test
  void confirmarPersisteLoMostradoYRegistraElAviso() {
    var huella = servicio.calcularVistaPrevia(previa(PRECIO)).huellaDatos();
    when(liquidaciones.guardarActiva(any(), any(), eq(USUARIO), eq(AHORA))).thenReturn(42L);
    var esperado = mock(LiquidacionDto.class);
    when(consultar.consultarLiquidacion(42L)).thenReturn(esperado);

    var generada =
        servicio.generarLiquidacion(
            new GenerarLiquidacionCommand(DatosPrueba.ID_LOTE, PRECIO, USUARIO, huella));

    assertThat(generada).isSameAs(esperado);
    var calculo = ArgumentCaptor.forClass(CalculoLiquidacion.class);
    verify(liquidaciones)
        .guardarActiva(any(DatosLiquidacion.class), calculo.capture(), eq(USUARIO), eq(AHORA));
    assertThat(calculo.getValue().ventaBrutaCop()).isEqualTo(107_100_000);
    verify(avisos).registrarUtilizacion(DatosPrueba.ID_RESULTADO, 42L, AHORA);
  }

  @Test
  void confirmarConHuellaDistintaResponde409YNoPersiste() {
    var comando =
        new GenerarLiquidacionCommand(DatosPrueba.ID_LOTE, PRECIO, USUARIO, "huella-vieja");

    assertThatThrownBy(() -> servicio.generarLiquidacion(comando))
        .isInstanceOf(VistaPreviaDesactualizadaException.class);
    verify(liquidaciones, never()).guardarActiva(any(), any(), any(), any());
  }

  @Test
  void confirmarRepiteLasValidaciones() {
    var huella = servicio.calcularVistaPrevia(previa(PRECIO)).huellaDatos();
    when(liquidaciones.buscarIdActivaPorLote(DatosPrueba.ID_LOTE)).thenReturn(Optional.of(7L));

    assertThatThrownBy(
            () ->
                servicio.generarLiquidacion(
                    new GenerarLiquidacionCommand(DatosPrueba.ID_LOTE, PRECIO, USUARIO, huella)))
        .isInstanceOf(LiquidacionYaExistenteException.class);
  }

  @Test
  void siniestroTotalNoEmiteAvisoDeUtilizacion() {
    datosDelCasoDorado(0);
    when(copiaLocal.buscarResultadoSacrificio(DatosPrueba.ID_LOTE)).thenReturn(Optional.empty());
    var huella = servicio.calcularVistaPrevia(previa(null)).huellaDatos();
    when(liquidaciones.guardarActiva(any(), any(), any(), any())).thenReturn(43L);

    servicio.generarLiquidacion(
        new GenerarLiquidacionCommand(DatosPrueba.ID_LOTE, null, USUARIO, huella));

    verify(avisos, never()).registrarUtilizacion(any(), anyLong(), any());
  }

  private void assertCausa(CausaNoLiquidable causa) {
    assertThatThrownBy(() -> servicio.calcularVistaPrevia(previa(PRECIO)))
        .isInstanceOf(LoteNoLiquidableException.class)
        .extracting("causa")
        .isEqualTo(causa);
  }
}
