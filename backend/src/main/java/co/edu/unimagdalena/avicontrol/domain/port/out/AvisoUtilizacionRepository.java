package co.edu.unimagdalena.avicontrol.domain.port.out;

import java.time.Instant;
import java.util.UUID;

/** Outbox del aviso de utilización del resultado de sacrificio para Módulo 2 (plan 001). */
public interface AvisoUtilizacionRepository {

  /**
   * Marca el resultado como utilizado y deja el aviso pendiente de envío. Si el resultado ya tenía
   * aviso (anular y volver a liquidar), se conserva el original.
   */
  void registrarUtilizacion(UUID idResultado, long idLiquidacion, Instant fechaHora);
}
