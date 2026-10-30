package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Lectura de la bitácora {@code registro_sincronizacion}. */
@Repository
public class RegistroSincronizacionJdbcAdapter implements RegistroSincronizacionRepository {

  private final JdbcClient jdbc;

  public RegistroSincronizacionJdbcAdapter(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Instant> ultimaSincronizacionExitosa(FuenteSincronizacion fuente) {
    OffsetDateTime ultima =
        jdbc.sql(
                """
                SELECT MAX(fecha_hora_fin) FROM registro_sincronizacion
                 WHERE fuente = :fuente AND resultado = 'EXITOSA'
                """)
            .param("fuente", fuente.name())
            .query((rs, fila) -> rs.getObject(1, OffsetDateTime.class))
            .single();
    return Optional.ofNullable(ultima).map(OffsetDateTime::toInstant);
  }
}
