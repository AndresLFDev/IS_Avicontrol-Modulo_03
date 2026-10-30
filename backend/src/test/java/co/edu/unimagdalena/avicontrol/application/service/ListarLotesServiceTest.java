package co.edu.unimagdalena.avicontrol.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.ParametroInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.model.LoteListado;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarEstadoSincronizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto.EstadoFuente;
import co.edu.unimagdalena.avicontrol.domain.port.out.LoteRepository;
import co.edu.unimagdalena.avicontrol.domain.shared.Pagina;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plan 002, T003: reglas de M3-CU01 en el servicio, con el repositorio simulado. */
class ListarLotesServiceTest {

  private static final EstadoSincronizacionDto SYNC =
      new EstadoSincronizacionDto(
          new EstadoFuente(Instant.parse("2026-10-20T13:00:01Z"), false),
          new EstadoFuente(Instant.parse("2026-10-20T14:20:01Z"), false));

  private LoteRepository repositorio;
  private ListarLotesService servicio;

  @BeforeEach
  void configurar() {
    repositorio = mock(LoteRepository.class);
    var estado = mock(ConsultarEstadoSincronizacionUseCase.class);
    when(estado.consultar()).thenReturn(SYNC);
    servicio = new ListarLotesService(repositorio, estado);
  }

  @Test
  void rechazaLaEtapaLiquidado() {
    assertThatThrownBy(() -> servicio.listarLotes(EtapaLote.LIQUIDADO, null, 0, 6))
        .isInstanceOf(ParametroInvalidoException.class)
        .extracting("campo")
        .isEqualTo("etapa");
    verify(repositorio, never()).buscarListables(any(), any(), anyInt(), anyInt());
  }

  @Test
  void limitaLaPaginaASeisLotes() {
    when(repositorio.buscarListables(isNull(), isNull(), eq(0), eq(6)))
        .thenReturn(Pagina.de(List.of(), 0, 6, 0));

    var pagina = servicio.listarLotes(null, null, 0, 50);

    assertThat(pagina.size()).isEqualTo(6);
    verify(repositorio).buscarListables(null, null, 0, 6);
  }

  @Test
  void informaCuantosLotesMuestraFrenteAlTotal() {
    var seis = List.of(lote(EtapaLote.PRODUCTIVO, 100, false));
    when(repositorio.buscarListables(null, null, 1, 6)).thenReturn(Pagina.de(seis, 1, 6, 7));

    var pagina = servicio.listarLotes(null, null, 1, 6);

    assertThat(pagina.mensaje()).isEqualTo("1 de 7 lotes");
    assertThat(pagina.totalElements()).isEqualTo(7);
    assertThat(pagina.totalPages()).isEqualTo(2);
    assertThat(pagina.sincronizacion()).isEqualTo(SYNC);
  }

  @Test
  void sinLotesListablesMuestraElMensajeDeVacioTotal() {
    when(repositorio.buscarListables(null, null, 0, 6)).thenReturn(Pagina.de(List.of(), 0, 6, 0));
    when(repositorio.existeAlgunoListable()).thenReturn(false);

    assertThat(servicio.listarLotes(null, "  ", 0, 6).mensaje())
        .isEqualTo("No hay lotes disponibles para consultar");
  }

  @Test
  void filtroSinCoincidenciasMuestraSinResultados() {
    when(repositorio.buscarListables(EtapaLote.AISLAMIENTO, null, 0, 6))
        .thenReturn(Pagina.de(List.of(), 0, 6, 0));

    assertThat(servicio.listarLotes(EtapaLote.AISLAMIENTO, null, 0, 6).mensaje())
        .isEqualTo("No se encontraron resultados");
  }

  @Test
  void paginaFueraDeRangoMuestraSinResultadosYConservaElTotal() {
    when(repositorio.buscarListables(null, null, 5, 6)).thenReturn(Pagina.de(List.of(), 5, 6, 7));
    when(repositorio.existeAlgunoListable()).thenReturn(true);

    var pagina = servicio.listarLotes(null, null, 5, 6);

    assertThat(pagina.mensaje()).isEqualTo("No se encontraron resultados");
    assertThat(pagina.totalElements()).isEqualTo(7);
  }

  @Test
  void recortaLaBusqueda() {
    when(repositorio.buscarListables(null, "norte", 0, 6))
        .thenReturn(Pagina.de(List.of(), 0, 6, 0));

    servicio.listarLotes(null, "  norte ", 0, 6);

    verify(repositorio).buscarListables(null, "norte", 0, 6);
  }

  @Test
  void habilitaGenerarSoloEnPorLiquidarConResultadoOSiniestro() {
    var filas =
        List.of(
            lote(EtapaLote.POR_LIQUIDAR, 8500, true), // con resultado de sacrificio
            lote(EtapaLote.POR_LIQUIDAR, 0, false), // siniestro total
            lote(EtapaLote.POR_LIQUIDAR, 8650, false), // sin resultado
            lote(EtapaLote.PRODUCTIVO, 9420, true),
            lote(EtapaLote.EN_COSECHA, 9550, false),
            lote(EtapaLote.AISLAMIENTO, 0, false));
    when(repositorio.buscarListables(null, null, 0, 6)).thenReturn(Pagina.de(filas, 0, 6, 6));

    var contenido = servicio.listarLotes(null, null, 0, 6).content();

    assertThat(contenido)
        .extracting(f -> f.accionHabilitada())
        .containsExactly(true, true, false, false, false, false);
    assertThat(contenido).allSatisfy(f -> assertThat(f.accion()).isEqualTo("GENERAR_LIQUIDACION"));
  }

  @Test
  void marcaSiniestroTotalCuandoLaPoblacionActualEsCero() {
    var filas =
        List.of(lote(EtapaLote.POR_LIQUIDAR, 0, false), lote(EtapaLote.PRODUCTIVO, 1, false));
    when(repositorio.buscarListables(null, null, 0, 6)).thenReturn(Pagina.de(filas, 0, 6, 2));

    var contenido = servicio.listarLotes(null, null, 0, 6).content();

    assertThat(contenido).extracting(f -> f.siniestroTotal()).containsExactly(true, false);
    assertThat(contenido).extracting(f -> f.etapa()).containsExactly("POR_LIQUIDAR", "PRODUCTIVO");
  }

  private static LoteListado lote(EtapaLote etapa, int poblacionActual, boolean conResultado) {
    return new LoteListado(
        UUID.randomUUID(),
        "L-" + etapa,
        LocalDate.of(2026, 8, 1),
        UUID.randomUUID(),
        "Galpón " + etapa,
        etapa,
        poblacionActual,
        conResultado);
  }
}
