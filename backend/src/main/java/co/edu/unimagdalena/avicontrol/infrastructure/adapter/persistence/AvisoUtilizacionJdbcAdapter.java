package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Outbox del aviso de utilización: el envío a Kafka lo hace el publicador programado (plan 001).
 */
@Repository
public class AvisoUtilizacionJdbcAdapter implements AvisoUtilizacionRepository {

  private final JdbcClient jdbc;

  public AvisoUtilizacionJdbcAdapter(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void registrarUtilizacion(UUID idResultado, long idLiquidacion, Instant fechaHora) {
    jdbc.sql(
            """
            UPDATE resultado_final_sacrificio
               SET estado_utilizacion = 'UTILIZADO'
             WHERE id_resultado = :idResultado
            """)
        .param("idResultado", idResultado)
        .update();
    // id_resultado es UNIQUE: tras anular y volver a liquidar se conserva el aviso original
    jdbc.sql(
            """
            INSERT INTO aviso_utilizacion_resultado (id_resultado, id_liquidacion, fecha_hora_emision)
            VALUES (:idResultado, :idLiquidacion, :fechaHora)
            ON CONFLICT (id_resultado) DO NOTHING
            """)
        .param("idResultado", idResultado)
        .param("idLiquidacion", idLiquidacion)
        .param("fechaHora", fechaHora.atOffset(ZoneOffset.UTC))
        .update();
  }
}
