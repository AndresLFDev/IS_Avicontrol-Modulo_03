package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unimagdalena.avicontrol.domain.exception.ParametroInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.port.in.ListarLotesUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto.EstadoFuente;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LotePageDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LoteResumenDto;
import co.edu.unimagdalena.avicontrol.infrastructure.exception.GlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Plan 002, T007: contrato REST de {@code GET /api/v1/lotes}. */
class LoteControllerTest {

  private static final Clock RELOJ =
      Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneOffset.UTC);

  private ListarLotesUseCase casoDeUso;
  private MockMvc mvc;

  @BeforeEach
  void configurar() {
    casoDeUso = mock(ListarLotesUseCase.class);
    var validador = new LocalValidatorFactoryBean();
    validador.afterPropertiesSet();
    mvc =
        MockMvcBuilders.standaloneSetup(new LoteController(casoDeUso))
            .setControllerAdvice(new GlobalExceptionHandler(RELOJ))
            .setValidator(validador)
            .build();
  }

  @Test
  void respondeLaPaginaConElFormatoDelContrato() throws Exception {
    var fila =
        new LoteResumenDto(
            UUID.fromString("b0000000-0000-0000-0000-000000000003"),
            "L-2026-03",
            LocalDate.of(2026, 8, 1),
            UUID.fromString("a0000000-0000-0000-0000-000000000003"),
            "Galpón Este",
            "POR_LIQUIDAR",
            false,
            "GENERAR_LIQUIDACION",
            true);
    var sync =
        new EstadoSincronizacionDto(
            new EstadoFuente(Instant.parse("2026-10-20T13:07:01Z"), false),
            new EstadoFuente(null, true));
    when(casoDeUso.listarLotes(EtapaLote.POR_LIQUIDAR, "este", 0, 6))
        .thenReturn(new LotePageDto(List.of(fila), 0, 6, 1, 1, "1 de 1 lotes", sync));

    mvc.perform(get("/api/v1/lotes").param("etapa", "POR_LIQUIDAR").param("search", "este"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].idLote").value("b0000000-0000-0000-0000-000000000003"))
        .andExpect(jsonPath("$.content[0].fechaIngreso").value("2026-08-01"))
        .andExpect(jsonPath("$.content[0].etapa").value("POR_LIQUIDAR"))
        .andExpect(jsonPath("$.content[0].accion").value("GENERAR_LIQUIDACION"))
        .andExpect(jsonPath("$.content[0].accionHabilitada").value(true))
        .andExpect(jsonPath("$.mensaje").value("1 de 1 lotes"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(
            jsonPath("$.sincronizacion.modulo1.ultimaSincronizacionExitosa")
                .value("2026-10-20T13:07:01Z"))
        .andExpect(jsonPath("$.sincronizacion.modulo2.desactualizada").value(true));
  }

  @Test
  void sinParametrosUsaPaginaCeroYTamanoSeis() throws Exception {
    when(casoDeUso.listarLotes(isNull(), isNull(), anyInt(), anyInt()))
        .thenReturn(new LotePageDto(List.of(), 0, 6, 0, 0, "No hay lotes", null));

    mvc.perform(get("/api/v1/lotes")).andExpect(status().isOk());

    verify(casoDeUso).listarLotes(null, null, 0, 6);
  }

  @Test
  void etapaDesconocidaResponde400() throws Exception {
    mvc.perform(get("/api/v1/lotes").param("etapa", "VENDIDO"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/validacion"))
        .andExpect(jsonPath("$.errores[0].campo").value("etapa"));
  }

  @Test
  void etapaLiquidadoResponde400() throws Exception {
    when(casoDeUso.listarLotes(any(), any(), anyInt(), anyInt()))
        .thenThrow(new ParametroInvalidoException("etapa", "La etapa LIQUIDADO no se lista."));

    mvc.perform(get("/api/v1/lotes").param("etapa", "LIQUIDADO"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("etapa"))
        .andExpect(jsonPath("$.errores[0].mensaje").value("La etapa LIQUIDADO no se lista."));
  }

  @Test
  void paginaNegativaResponde400() throws Exception {
    mvc.perform(get("/api/v1/lotes").param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("page"));
  }

  @Test
  void tamanoCeroResponde400() throws Exception {
    mvc.perform(get("/api/v1/lotes").param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("size"));
  }

  @Test
  void busquedaDeMasDeCienCaracteresResponde400() throws Exception {
    mvc.perform(get("/api/v1/lotes").param("search", "x".repeat(101)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("search"));
  }
}
