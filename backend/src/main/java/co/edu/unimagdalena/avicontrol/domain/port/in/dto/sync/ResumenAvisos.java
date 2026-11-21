package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.util.List;

/**
 * Resultado de una ronda de confirmaciones a Módulo 2.
 *
 * @param entregados avisos que M2 confirmó
 * @param fallidos avisos que siguen pendientes por un error o falta de respuesta
 * @param errores causa de cada fallo
 */
public record ResumenAvisos(int entregados, int fallidos, List<String> errores) {

  public ResumenAvisos {
    errores = errores == null ? List.of() : List.copyOf(errores);
  }
}
