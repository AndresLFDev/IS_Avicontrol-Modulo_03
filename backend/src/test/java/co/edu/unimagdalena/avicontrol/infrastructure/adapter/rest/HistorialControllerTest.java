package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unimagdalena.avicontrol.domain.exception.RangoFechasInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarHistorialUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialItemDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialPageDto;
import co.edu.unimagdalena.avicontrol.infrastructure.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
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

/** Plan 004, T004: contrato REST de {@code GET /api/v1/liquidaciones}. */
class HistorialControllerTest {

  private static final Clock RELOJ =
      Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneOffset.UTC);

  private ConsultarHistorialUseCase casoDeUso;
  private MockMvc mvc;

  @BeforeEach
  void configurar() {
    casoDeUso = mock(ConsultarHistorialUseCase.class);
    var validador = new LocalValidatorFactoryBean();
    validador.afterPropertiesSet();
    mvc =
        MockMvcBuilders.standaloneSetup(new HistorialController(casoDeUso))
            .setControllerAdvice(new GlobalExceptionHandler(RELOJ))
            .setValidator(validador)
            .build();
  }

  @Test
  void respondeLaPaginaConElFormatoDelContrato() throws Exception {
    var desde = LocalDate.of(2026, 7, 1);
    var hasta = LocalDate.of(2026, 9, 30);
    var fila =
        new HistorialItemDto(
            40,
            Instant.parse("2026-09-20T09:15:00Z"),
            UUID.fromString("f1e2d3c4-e89b-12d3-a456-426614174000"),
            "Galpón Norte",
            UUID.fromString("a1b2c3d4-e89b-12d3-a456-426614174000"),
            "Lote Norte Ciclo 4",
            132_000_000,
            new BigDecimal("4.00"),
            60_200_000,
            71_800_000,
            "ANULADA");
    when(casoDeUso.consultarHistorial("norte", desde, hasta, 0, 10))
        .thenReturn(new HistorialPageDto(List.of(fila), 0, 10, 1, 1, null));

    mvc.perform(
            get("/api/v1/liquidaciones")
                .param("search", "norte")
                .param("desde", "2026-07-01")
                .param("hasta", "2026-09-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].idLiquidacion").value(40))
        .andExpect(jsonPath("$.content[0].fechaHoraGeneracion").value("2026-09-20T09:15:00Z"))
        .andExpect(jsonPath("$.content[0].nombreGalpon").value("Galpón Norte"))
        .andExpect(jsonPath("$.content[0].idLote").value("a1b2c3d4-e89b-12d3-a456-426614174000"))
        .andExpect(jsonPath("$.content[0].nombreLote").value("Lote Norte Ciclo 4"))
        .andExpect(jsonPath("$.content[0].ventaBrutaCop").value(132_000_000))
        .andExpect(jsonPath("$.content[0].porcentajeMortalidad").value(4.00))
        .andExpect(jsonPath("$.content[0].costosOperativosCop").value(60_200_000))
        .andExpect(jsonPath("$.content[0].utilidadNetaCop").value(71_800_000))
        .andExpect(jsonPath("$.content[0].estado").value("ANULADA"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.mensaje").isEmpty());
  }

  @Test
  void sinParametrosUsaPaginaCeroYTamanoDiez() throws Exception {
    when(casoDeUso.consultarHistorial(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(new HistorialPageDto(List.of(), 0, 10, 0, 0, "Sin datos"));

    mvc.perform(get("/api/v1/liquidaciones")).andExpect(status().isOk());

    verify(casoDeUso).consultarHistorial(null, null, null, 0, 10);
  }

  @Test
  void rangoInvertidoResponde400ConSuTipoPropio() throws Exception {
    var desde = LocalDate.of(2026, 9, 30);
    var hasta = LocalDate.of(2026, 9, 1);
    when(casoDeUso.consultarHistorial(null, desde, hasta, 0, 10))
        .thenThrow(new RangoFechasInvalidoException(desde, hasta));

    mvc.perform(
            get("/api/v1/liquidaciones").param("desde", "2026-09-30").param("hasta", "2026-09-01"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.type").value("https://avicontrol.edu.co/errors/rango-fechas-invalido"))
        .andExpect(jsonPath("$.title").value("Rango de fechas inválido"))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "La fecha inicial (2026-09-30) debe ser menor o igual a la fecha final"
                        + " (2026-09-01)."))
        .andExpect(jsonPath("$.instance").value("/api/v1/liquidaciones"));
  }

  @Test
  void parametrosInvalidosResponden400SinConsultar() throws Exception {
    String[][] casos = {
      {"page", "-1"},
      {"size", "0"},
      {"size", "51"},
      {"desde", "30-09-2026"},
      {"hasta", "no-es-fecha"},
      {"search", "x".repeat(101)},
    };
    for (String[] caso : casos) {
      mvc.perform(get("/api/v1/liquidaciones").param(caso[0], caso[1]))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/validacion"))
          .andExpect(jsonPath("$.errores[0].campo").value(caso[0]));
    }
    verifyNoInteractions(casoDeUso);
  }
}
