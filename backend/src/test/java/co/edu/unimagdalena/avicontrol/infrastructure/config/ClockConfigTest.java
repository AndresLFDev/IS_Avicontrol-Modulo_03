package co.edu.unimagdalena.avicontrol.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** El reloj de la aplicación es UTC y real: las fechas de auditoría se guardan en UTC. */
class ClockConfigTest {

  @Test
  void elRelojEsUtc() {
    Clock reloj = new ClockConfig().clock();

    assertThat(reloj.getZone()).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  void elRelojMarcaLaHoraActual() {
    Clock reloj = new ClockConfig().clock();

    Instant antes = Instant.now();
    Instant leida = reloj.instant();
    Instant despues = Instant.now();

    assertThat(leida).isBetween(antes, despues);
  }
}
