package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarEstadoSincronizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto.EstadoFuente;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** RT-09: contrato REST de {@code GET /api/v1/sincronizacion/estado}. */
class SincronizacionControllerTest {

  private ConsultarEstadoSincronizacionUseCase casoDeUso;
  private MockMvc mvc;

  @BeforeEach
  void configurar() {
    casoDeUso = mock(ConsultarEstadoSincronizacionUseCase.class);
    mvc = MockMvcBuilders.standaloneSetup(new SincronizacionController(casoDeUso)).build();
  }

  @Test
  void respondeElEstadoDeAmbasFuentes() throws Exception {
    when(casoDeUso.consultar())
        .thenReturn(
            new EstadoSincronizacionDto(
                new EstadoFuente(Instant.parse("2026-10-20T13:07:01Z"), false),
                new EstadoFuente(Instant.parse("2026-10-20T14:20:01Z"), true)));

    mvc.perform(get("/api/v1/sincronizacion/estado"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.modulo1.ultimaSincronizacionExitosa").value("2026-10-20T13:07:01Z"))
        .andExpect(jsonPath("$.modulo1.desactualizada").value(false))
        .andExpect(jsonPath("$.modulo2.ultimaSincronizacionExitosa").value("2026-10-20T14:20:01Z"))
        .andExpect(jsonPath("$.modulo2.desactualizada").value(true));
  }

  @Test
  void unaFuenteSinEventosExitososSeInformaSinFechaYDesactualizada() throws Exception {
    when(casoDeUso.consultar())
        .thenReturn(
            new EstadoSincronizacionDto(
                new EstadoFuente(null, true),
                new EstadoFuente(Instant.parse("2026-10-20T14:20:01Z"), false)));

    mvc.perform(get("/api/v1/sincronizacion/estado"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.modulo1.ultimaSincronizacionExitosa").isEmpty())
        .andExpect(jsonPath("$.modulo1.desactualizada").value(true))
        .andExpect(jsonPath("$.modulo2.desactualizada").value(false));
  }
}
