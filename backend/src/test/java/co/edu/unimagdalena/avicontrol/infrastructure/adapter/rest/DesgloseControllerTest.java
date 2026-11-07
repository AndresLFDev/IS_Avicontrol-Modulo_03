package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unimagdalena.avicontrol.domain.exception.ExportacionFallidaException;
import co.edu.unimagdalena.avicontrol.domain.exception.ExportacionNoPermitidaException;
import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarDesgloseUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.ExportarExcelUseCase;
import co.edu.unimagdalena.avicontrol.infrastructure.exception.GlobalExceptionHandler;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Plan 004, T008 y T011: cabeceras del archivo y errores del contrato REST. */
class DesgloseControllerTest {

  private static final String TIPO = "https://avicontrol.edu.co/errors/";
  private static final String EXCEL = "/api/v1/liquidaciones/42/desglose/excel";

  private final ConsultarDesgloseUseCase consultar = mock(ConsultarDesgloseUseCase.class);
  private final ExportarExcelUseCase exportar = mock(ExportarExcelUseCase.class);
  private MockMvc mvc;

  @BeforeEach
  void configurar() {
    var reloj = Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC);
    mvc =
        MockMvcBuilders.standaloneSetup(new DesgloseController(consultar, exportar))
            .setControllerAdvice(new GlobalExceptionHandler(reloj))
            .build();
  }

  @Test
  void entregaElArchivoComoAdjuntoXlsx() throws Exception {
    byte[] archivo = {1, 2, 3};
    when(exportar.generarReporteExcelLiquidacion(42L)).thenReturn(archivo);

    mvc.perform(get(EXCEL))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string("Content-Disposition", "attachment; filename=\"Liquidacion_Lote_42.xlsx\""))
        .andExpect(
            content()
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .andExpect(content().bytes(archivo));
  }

  @Test
  void unaAnuladaResponde409() throws Exception {
    when(exportar.generarReporteExcelLiquidacion(42L))
        .thenThrow(new ExportacionNoPermitidaException(42L));

    mvc.perform(get(EXCEL))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value(TIPO + "exportacion-no-permitida"));
  }

  @Test
  void unaInexistenteResponde404() throws Exception {
    when(consultar.consultarDesglose(42L)).thenThrow(new LiquidacionNoEncontradaException(42L));

    mvc.perform(get("/api/v1/liquidaciones/42/desglose"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value(TIPO + "liquidacion-no-encontrada"));
  }

  @Test
  void unaFallaAlGenerarRespondeErrorInternoSinExponerLaCausa() throws Exception {
    when(exportar.generarReporteExcelLiquidacion(42L))
        .thenThrow(new ExportacionFallidaException(42L, new IOException("disco lleno")));

    mvc.perform(get(EXCEL))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.type").value(TIPO + "error-interno"))
        .andExpect(
            jsonPath("$.detail").value("Ocurrió un error inesperado. Intente de nuevo más tarde."));
  }
}
