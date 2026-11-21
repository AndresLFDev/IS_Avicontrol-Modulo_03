package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.AvisoPendiente;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Outbox del aviso de utilización del resultado de sacrificio para Módulo 2 (plan 001). */
public interface AvisoUtilizacionRepository {

  /**
   * Marca el resultado como utilizado y deja el aviso pendiente de envío. Si el resultado ya tenía
   * aviso (anular y volver a liquidar), se conserva el original.
   */
  void registrarUtilizacion(UUID idResultado, long idLiquidacion, Instant fechaHora);

  /** Avisos que M2 aún no confirmó, del más antiguo al más reciente. */
  List<AvisoPendiente> pendientes();

  /** Marca el aviso como entregado (M2 respondió 200) y cuenta el intento. */
  void marcarEntregado(long idAviso);

  /** Cuenta un intento que no logró la confirmación; el aviso sigue pendiente. */
  void registrarIntentoFallido(long idAviso);
}
