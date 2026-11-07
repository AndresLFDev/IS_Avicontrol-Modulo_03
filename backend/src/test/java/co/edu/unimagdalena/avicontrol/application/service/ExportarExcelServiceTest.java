package co.edu.unimagdalena.avicontrol.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.ExportacionNoPermitidaException;
import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarDesgloseUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.IngresosDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.GeneradorExcelPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Plan 004, T009: solo se exporta una Liquidación ACTIVA (CU05.FR-004). */
class ExportarExcelServiceTest {

  private final ConsultarDesgloseUseCase consultar = mock(ConsultarDesgloseUseCase.class);
  private final GeneradorExcelPort generador = mock(GeneradorExcelPort.class);
  private final ExportarExcelService servicio = new ExportarExcelService(consultar, generador);

  @Test
  void generaElLibroConElDesgloseDeLaPantalla() {
    var desglose = desglose(true);
    byte[] archivo = {1, 2, 3};
    when(consultar.consultarDesglose(42L)).thenReturn(desglose);
    when(generador.generar(desglose)).thenReturn(archivo);

    assertThat(servicio.generarReporteExcelLiquidacion(42L)).isSameAs(archivo);
  }

  @Test
  void rechazaUnaAnulada() {
    when(consultar.consultarDesglose(42L)).thenReturn(desglose(false));

    assertThatThrownBy(() -> servicio.generarReporteExcelLiquidacion(42L))
        .isInstanceOf(ExportacionNoPermitidaException.class);
    verify(generador, never()).generar(any());
  }

  @Test
  void unaInexistenteResponde404() {
    when(consultar.consultarDesglose(999L)).thenThrow(new LiquidacionNoEncontradaException(999L));

    assertThatThrownBy(() -> servicio.generarReporteExcelLiquidacion(999L))
        .isInstanceOf(LiquidacionNoEncontradaException.class);
    verify(generador, never()).generar(any());
  }

  private static DesgloseDto desglose(boolean exportable) {
    return new DesgloseDto(
        42L,
        exportable ? "ACTIVA" : "ANULADA",
        UUID.randomUUID(),
        "L-2026-03",
        "Galpón Este",
        false,
        new IngresosDto(1, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1),
        List.of(),
        0,
        BigDecimal.ZERO,
        0,
        1,
        null,
        exportable);
  }
}
