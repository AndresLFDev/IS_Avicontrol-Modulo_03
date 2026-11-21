package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenSincronizacion;

/**
 * M3-CU08 a CU10: consulta a M2 el resultado final, el alimento y el medicamento de los lotes por
 * liquidar y mantiene la copia local (plan 001).
 */
public interface SincronizarModulo2UseCase {

  /**
   * Consulta a M2 y actualiza la copia local. Deja una fila en la bitácora por ejecución. Nunca
   * lanza por un fallo de M2: lo registra como {@code FALLIDA} y conserva la copia anterior.
   */
  ResumenSincronizacion sincronizar();
}
