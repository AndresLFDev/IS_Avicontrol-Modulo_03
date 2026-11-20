package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import java.time.Instant;
import java.util.Optional;

/** Bitácora de sincronización con Módulo 1 y Módulo 2: una fila por ejecución (RT-09, plan 001). */
public interface RegistroSincronizacionRepository {

  /** Fin de la última sincronización exitosa de la fuente; vacío si nunca hubo una. */
  Optional<Instant> ultimaSincronizacionExitosa(FuenteSincronizacion fuente);

  /** Verdadero si la última ejecución terminada de la fuente fue {@code FALLIDA}. */
  boolean ultimaEjecucionFallo(FuenteSincronizacion fuente);

  /**
   * Abre una ejecución y devuelve su id. Queda sin resultado ni fin hasta {@link #completar} o
   * {@link #fallar}.
   */
  long iniciar(FuenteSincronizacion fuente, String ruta, Instant inicio);

  /**
   * Cierra la ejecución como {@code EXITOSA}; {@code nota} lleva los datos omitidos, si los hay.
   */
  void completar(long idRegistro, int registrosActualizados, String nota, Instant fin);

  /** Cierra la ejecución como {@code FALLIDA} con su causa. */
  void fallar(long idRegistro, String causa, Instant fin);
}
