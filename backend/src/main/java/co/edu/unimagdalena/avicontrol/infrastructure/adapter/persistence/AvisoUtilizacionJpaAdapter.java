package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Outbox del aviso de utilización: el envío a Kafka lo hace el publicador programado (plan 001).
 */
@Repository
public class AvisoUtilizacionJpaAdapter implements AvisoUtilizacionRepository {

  private static final String UTILIZADO = "UTILIZADO";

  private final ResultadoFinalSacrificioJpaRepository resultados;
  private final AvisoUtilizacionJpaRepository avisos;

  public AvisoUtilizacionJpaAdapter(
      ResultadoFinalSacrificioJpaRepository resultados, AvisoUtilizacionJpaRepository avisos) {
    this.resultados = resultados;
    this.avisos = avisos;
  }

  @Override
  public void registrarUtilizacion(UUID idResultado, long idLiquidacion, Instant fechaHora) {
    resultados.findById(idResultado).ifPresent(r -> r.estadoUtilizacion = UTILIZADO);
    // id_resultado es UNIQUE: tras anular y volver a liquidar se conserva el aviso original
    avisos.insertarSiNoExiste(idResultado, idLiquidacion, fechaHora);
  }
}
