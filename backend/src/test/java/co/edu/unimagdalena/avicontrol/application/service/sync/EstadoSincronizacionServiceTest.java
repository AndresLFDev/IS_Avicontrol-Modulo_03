package co.edu.unimagdalena.avicontrol.application.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Plan 001, T017: indicador de antigüedad de la sincronización (RT-09). */
class EstadoSincronizacionServiceTest {

  private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

  @Test
  void marcaDesactualizadaLaFuenteFueraDelUmbralOSinEventos() {
    var registros = mock(RegistroSincronizacionRepository.class);
    when(registros.ultimaSincronizacionExitosa(FuenteSincronizacion.MODULO_1))
        .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(2))));
    when(registros.ultimaSincronizacionExitosa(FuenteSincronizacion.MODULO_2))
        .thenReturn(Optional.empty());
    var servicio =
        new EstadoSincronizacionService(
            registros, Clock.fixed(AHORA, ZoneOffset.UTC), Duration.ofHours(24));

    var estado = servicio.consultar();

    assertThat(estado.modulo1().ultimaSincronizacionExitosa())
        .isEqualTo(AHORA.minus(Duration.ofHours(2)));
    assertThat(estado.modulo1().desactualizada()).isFalse();
    assertThat(estado.modulo2().ultimaSincronizacionExitosa()).isNull();
    assertThat(estado.modulo2().desactualizada()).isTrue();
  }

  @Test
  void unaFuenteMasAntiguaQueElUmbralEstaDesactualizada() {
    var registros = mock(RegistroSincronizacionRepository.class);
    when(registros.ultimaSincronizacionExitosa(FuenteSincronizacion.MODULO_1))
        .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(25))));
    when(registros.ultimaSincronizacionExitosa(FuenteSincronizacion.MODULO_2))
        .thenReturn(Optional.of(AHORA));
    var servicio =
        new EstadoSincronizacionService(
            registros, Clock.fixed(AHORA, ZoneOffset.UTC), Duration.ofHours(24));

    var estado = servicio.consultar();

    assertThat(estado.modulo1().desactualizada()).isTrue();
    assertThat(estado.modulo2().desactualizada()).isFalse();
  }
}
