package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenAvisos;

/** Confirma a Módulo 2 los resultados de sacrificio que M3 ya utilizó en una Liquidación. */
public interface PublicarAvisosUtilizacionUseCase {

  /**
   * Envía los avisos pendientes. Un aviso que falla queda pendiente y se reintenta con el mismo
   * identificador de evento. Nunca lanza por un fallo de M2.
   */
  ResumenAvisos publicar();
}
