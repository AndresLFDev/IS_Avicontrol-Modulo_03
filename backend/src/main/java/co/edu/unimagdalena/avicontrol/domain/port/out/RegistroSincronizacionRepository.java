package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import java.time.Instant;
import java.util.Optional;

/** Lectura de la bitácora de sincronización con Módulo 1 y Módulo 2 (RT-09). */
public interface RegistroSincronizacionRepository {

  /** Fin de la última sincronización exitosa de la fuente; vacío si nunca hubo una. */
  Optional<Instant> ultimaSincronizacionExitosa(FuenteSincronizacion fuente);
}
