package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unimagdalena.avicontrol.TestcontainersConfiguration;
import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * RT-09: última sincronización exitosa por fuente, incluida una fuente que aún no tiene ninguna.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RegistroSincronizacionJdbcAdapterIT {

  @Autowired private RegistroSincronizacionJdbcAdapter adaptador;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    jdbc.execute("TRUNCATE registro_sincronizacion RESTART IDENTITY CASCADE");
  }

  @Test
  void sinSincronizacionesExitosasNoHayFecha() {
    registro("MODULO_1", "2026-10-20 08:00:00-05", "FALLIDA");
    registro("MODULO_1", null, "EXITOSA"); // en curso, sin fin

    assertThat(adaptador.ultimaSincronizacionExitosa(FuenteSincronizacion.MODULO_1)).isEmpty();
    assertThat(adaptador.ultimaSincronizacionExitosa(FuenteSincronizacion.MODULO_2)).isEmpty();
  }

  @Test
  void devuelveLaMasRecienteDeLaFuente() {
    registro("MODULO_2", "2026-10-20 09:00:01-05", "EXITOSA");
    registro("MODULO_2", "2026-10-20 09:20:01-05", "EXITOSA");
    registro("MODULO_1", "2026-10-21 08:00:01-05", "EXITOSA");

    assertThat(adaptador.ultimaSincronizacionExitosa(FuenteSincronizacion.MODULO_2))
        .contains(Instant.parse("2026-10-20T14:20:01Z"));
  }

  private void registro(String fuente, String fin, String resultado) {
    jdbc.update(
        "INSERT INTO registro_sincronizacion (fuente, topico, fecha_hora_inicio, fecha_hora_fin,"
            + " resultado) VALUES (?, 'topico', now(), CAST(? AS TIMESTAMPTZ), ?)",
        fuente,
        fin,
        resultado);
  }
}
