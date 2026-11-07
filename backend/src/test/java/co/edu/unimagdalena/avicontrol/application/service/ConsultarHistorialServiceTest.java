package co.edu.unimagdalena.avicontrol.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.RangoFechasInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialItemDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import co.edu.unimagdalena.avicontrol.domain.shared.Pagina;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plan 004, T002: reglas de M3-CU06 en el servicio, con el repositorio simulado. */
class ConsultarHistorialServiceTest {

  private static final HistorialItemDto FILA =
      new HistorialItemDto(
          42,
          Instant.parse("2026-09-28T16:42:00Z"),
          UUID.randomUUID(),
          "Galpón Norte",
          UUID.randomUUID(),
          "L-01",
          139_200_000,
          new BigDecimal("4.00"),
          60_200_000,
          79_000_000,
          "ACTIVA");

  private LiquidacionRepository repositorio;
  private ConsultarHistorialService servicio;

  @BeforeEach
  void configurar() {
    repositorio = mock(LiquidacionRepository.class);
    servicio = new ConsultarHistorialService(repositorio);
  }

  private void conResultado(List<HistorialItemDto> filas, long total) {
    when(repositorio.buscarHistorial(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(Pagina.de(filas, 0, 10, total));
  }

  @Test
  void rechazaElRangoInvertidoSinConsultar() {
    var desde = LocalDate.of(2026, 9, 30);
    var hasta = LocalDate.of(2026, 9, 1);

    assertThatThrownBy(() -> servicio.consultarHistorial(null, desde, hasta, 0, 10))
        .isInstanceOf(RangoFechasInvalidoException.class)
        .hasMessage(
            "La fecha inicial (2026-09-30) debe ser menor o igual a la fecha final (2026-09-01).");
    verifyNoInteractions(repositorio);
  }

  @Test
  void aceptaUnRangoDeUnSoloDia() {
    conResultado(List.of(FILA), 1);
    var dia = LocalDate.of(2026, 9, 28);

    assertThat(servicio.consultarHistorial(null, dia, dia, 0, 10).content()).hasSize(1);
  }

  @Test
  void convierteElRangoALaZonaDeColombiaIncluyendoElDiaFinal() {
    conResultado(List.of(FILA), 1);

    servicio.consultarHistorial(null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 0, 10);

    // Colombia es UTC-5: el día empieza a las 05:00 UTC; el final es el inicio del día siguiente
    verify(repositorio)
        .buscarHistorial(
            null,
            Instant.parse("2026-09-01T05:00:00Z"),
            Instant.parse("2026-10-01T05:00:00Z"),
            0,
            10);
  }

  @Test
  void normalizaLaBusqueda() {
    conResultado(List.of(FILA), 1);

    servicio.consultarHistorial("  norte ", null, null, 0, 10);
    servicio.consultarHistorial("   ", null, null, 0, 10);

    verify(repositorio).buscarHistorial("norte", null, null, 0, 10);
    verify(repositorio).buscarHistorial(isNull(), isNull(), isNull(), anyInt(), anyInt());
  }

  @Test
  void conResultadosNoEnviaMensaje() {
    conResultado(List.of(FILA), 1);

    var pagina = servicio.consultarHistorial(null, null, null, 0, 10);

    assertThat(pagina.mensaje()).isNull();
    assertThat(pagina.content()).containsExactly(FILA);
    assertThat(pagina.totalElements()).isEqualTo(1);
    assertThat(pagina.totalPages()).isEqualTo(1);
  }

  @Test
  void sinLiquidacionesNiFiltrosInformaQueNoHayRegistros() {
    conResultado(List.of(), 0);

    assertThat(servicio.consultarHistorial(null, null, null, 0, 10).mensaje())
        .isEqualTo("No hay liquidaciones registradas aún");
  }

  @Test
  void conFiltrosSinCoincidenciasInformaQueNoSeEncontraron() {
    conResultado(List.of(), 0);
    String sinCoincidencias =
        "No se encontraron liquidaciones para los criterios de búsqueda aplicados";

    assertThat(servicio.consultarHistorial("x", null, null, 0, 10).mensaje())
        .isEqualTo(sinCoincidencias);
    assertThat(servicio.consultarHistorial(null, LocalDate.of(2026, 1, 1), null, 0, 10).mensaje())
        .isEqualTo(sinCoincidencias);
    assertThat(servicio.consultarHistorial(null, null, LocalDate.of(2026, 1, 1), 0, 10).mensaje())
        .isEqualTo(sinCoincidencias);
  }

  @Test
  void unaPaginaFueraDeRangoNoDiceQueNoHayRegistros() {
    conResultado(List.of(), 13);

    assertThat(servicio.consultarHistorial(null, null, null, 5, 10).mensaje())
        .isEqualTo("No se encontraron liquidaciones para los criterios de búsqueda aplicados");
  }
}
