package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.util.List;

/**
 * Resultado de una ejecución de la sincronización.
 *
 * @param exitosa falso si M1 no respondió o respondió con error; la copia local queda intacta
 * @param registrosActualizados galpones, lotes y alertas escritos en la copia local
 * @param incidencias datos omitidos por inválidos (CU07.FR-010 y FR-011)
 * @param error causa del fallo cuando {@code exitosa} es falso
 */
public record ResumenSincronizacion(
    boolean exitosa, int registrosActualizados, List<String> incidencias, String error) {

  public ResumenSincronizacion {
    incidencias = incidencias == null ? List.of() : List.copyOf(incidencias);
  }
}
