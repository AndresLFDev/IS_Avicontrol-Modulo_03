package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

/** Lectura de la bitácora {@code registro_sincronizacion}. */
@Repository
public class RegistroSincronizacionJpaAdapter implements RegistroSincronizacionRepository {

  private final RegistroSincronizacionJpaRepository repositorio;

  public RegistroSincronizacionJpaAdapter(RegistroSincronizacionJpaRepository repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public Optional<Instant> ultimaSincronizacionExitosa(FuenteSincronizacion fuente) {
    // Sin filas devuelve vacío: antes de la primera sincronización exitosa la fuente no tiene fecha
    return repositorio.fechasFinExitosas(fuente, Limit.of(1)).stream().findFirst();
  }
}
