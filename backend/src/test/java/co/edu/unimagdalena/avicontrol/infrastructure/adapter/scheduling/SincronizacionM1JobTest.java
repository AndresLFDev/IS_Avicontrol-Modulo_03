package co.edu.unimagdalena.avicontrol.infrastructure.adapter.scheduling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo1UseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenSincronizacion;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La tarea programada: sin URL no corre, y nunca propaga un fallo. */
class SincronizacionM1JobTest {

  @Test
  void sinUrlDeM1NoSincroniza() {
    var caso = mock(SincronizarModulo1UseCase.class);

    new SincronizacionM1Job(caso, "").ejecutar();

    verify(caso, never()).sincronizar();
  }

  @Test
  void conUrlSincroniza() {
    var caso = mock(SincronizarModulo1UseCase.class);
    when(caso.sincronizar()).thenReturn(new ResumenSincronizacion(true, 3, List.of(), null));

    new SincronizacionM1Job(caso, "http://m1/api/v1").ejecutar();

    verify(caso).sincronizar();
  }

  @Test
  void unFalloInesperadoNoSePropagaParaQueElSiguienteCicloReintente() {
    var caso = mock(SincronizarModulo1UseCase.class);
    when(caso.sincronizar()).thenThrow(new IllegalStateException("boom"));

    assertThatCode(() -> new SincronizacionM1Job(caso, "http://m1/api/v1").ejecutar())
        .doesNotThrowAnyException();
  }

  @Test
  void unaEjecucionFallidaTampocoLanza() {
    var caso = mock(SincronizarModulo1UseCase.class);
    when(caso.sincronizar())
        .thenReturn(new ResumenSincronizacion(false, 0, List.of(), "M1 sin respuesta"));

    assertThatCode(() -> new SincronizacionM1Job(caso, "http://m1/api/v1").ejecutar())
        .doesNotThrowAnyException();
  }
}
