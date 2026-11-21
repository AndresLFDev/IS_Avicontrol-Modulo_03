package co.edu.unimagdalena.avicontrol.infrastructure.adapter.scheduling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.port.in.PublicarAvisosUtilizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo2UseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenAvisos;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenSincronizacion;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Las tareas programadas de M2: sin URL no corren y nunca propagan un fallo. */
class TareasM2Test {

  @Test
  void laSincronizacionNoCorreSinUrlDeM2() {
    var caso = mock(SincronizarModulo2UseCase.class);

    new SincronizacionM2Job(caso, "").ejecutar();

    verify(caso, never()).sincronizar();
  }

  @Test
  void laSincronizacionCorreConUrl() {
    var caso = mock(SincronizarModulo2UseCase.class);
    when(caso.sincronizar()).thenReturn(new ResumenSincronizacion(true, 2, List.of(), null));

    new SincronizacionM2Job(caso, "https://m2/api/integracion/modulo3").ejecutar();

    verify(caso).sincronizar();
  }

  @Test
  void laSincronizacionNoPropagaFallosNiEjecucionesFallidas() {
    var caso = mock(SincronizarModulo2UseCase.class);
    when(caso.sincronizar()).thenThrow(new IllegalStateException("boom"));
    var job = new SincronizacionM2Job(caso, "https://m2");

    assertThatCode(job::ejecutar).doesNotThrowAnyException();

    doReturn(new ResumenSincronizacion(false, 0, List.of(), "sin M2")).when(caso).sincronizar();
    assertThatCode(job::ejecutar).doesNotThrowAnyException();
  }

  @Test
  void laPublicacionNoCorreSinUrlDeM2YLosAvisosQuedanGuardados() {
    var caso = mock(PublicarAvisosUtilizacionUseCase.class);

    new PublicacionAvisosM2Job(caso, "").ejecutar();

    verify(caso, never()).publicar();
  }

  @Test
  void laPublicacionCorreConUrlYNoPropagaFallos() {
    var caso = mock(PublicarAvisosUtilizacionUseCase.class);
    when(caso.publicar()).thenReturn(new ResumenAvisos(1, 1, List.of("x")));
    var job = new PublicacionAvisosM2Job(caso, "https://m2");

    assertThatCode(job::ejecutar).doesNotThrowAnyException();
    verify(caso).publicar();

    doThrow(new IllegalStateException("boom")).when(caso).publicar();
    assertThatCode(job::ejecutar).doesNotThrowAnyException();
  }
}
