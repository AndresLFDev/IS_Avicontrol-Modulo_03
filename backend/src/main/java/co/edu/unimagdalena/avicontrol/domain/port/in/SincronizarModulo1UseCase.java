package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenSincronizacion;

/** M3-CU07: sincroniza galpones, lotes y alertas de vaciado con Módulo 1 (plan 001). */
public interface SincronizarModulo1UseCase {

  /**
   * Consulta a M1 y actualiza la copia local. Deja una fila en la bitácora por ejecución. Nunca
   * lanza por un fallo de M1: lo registra como {@code FALLIDA} y conserva la copia anterior.
   */
  ResumenSincronizacion sincronizar();
}
