package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Lectura y escritura de la bitácora {@code registro_sincronizacion}. */
@Repository
public class RegistroSincronizacionJpaAdapter implements RegistroSincronizacionRepository {

  private static final int LIMITE_TEXTO = 4000;

  private final RegistroSincronizacionJpaRepository repositorio;

  public RegistroSincronizacionJpaAdapter(RegistroSincronizacionJpaRepository repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public Optional<Instant> ultimaSincronizacionExitosa(FuenteSincronizacion fuente) {
    // Sin filas devuelve vacío: antes de la primera sincronización exitosa la fuente no tiene fecha
    return repositorio.fechasFinExitosas(fuente, Limit.of(1)).stream().findFirst();
  }

  @Override
  public boolean ultimaEjecucionFallo(FuenteSincronizacion fuente) {
    return repositorio
        .findFirstByFuenteAndResultadoIsNotNullOrderByFechaHoraInicioDesc(fuente)
        .map(r -> "FALLIDA".equals(r.resultado))
        .orElse(false);
  }

  @Override
  @Transactional
  public long iniciar(FuenteSincronizacion fuente, String ruta, Instant inicio) {
    var registro = new RegistroSincronizacionEntity();
    registro.fuente = fuente;
    registro.topico = ruta;
    registro.fechaHoraInicio = inicio;
    registro.registrosActualizados = 0;
    return repositorio.save(registro).id;
  }

  @Override
  @Transactional
  public void completar(long idRegistro, int registrosActualizados, String nota, Instant fin) {
    var registro = cargar(idRegistro);
    registro.fechaHoraFin = fin;
    registro.resultado = "EXITOSA";
    registro.registrosActualizados = registrosActualizados;
    registro.descripcionError = recortar(nota);
  }

  @Override
  @Transactional
  public void fallar(long idRegistro, String causa, Instant fin) {
    var registro = cargar(idRegistro);
    registro.fechaHoraFin = fin;
    registro.resultado = "FALLIDA";
    registro.descripcionError = recortar(causa);
  }

  /**
   * Entidad gestionada real, no un proxy: los campos son de paquete y un proxy no los propagaría a
   * la base.
   */
  private RegistroSincronizacionEntity cargar(long idRegistro) {
    return repositorio
        .findById(idRegistro)
        .orElseThrow(
            () ->
                new IllegalStateException("No existe el registro de sincronización " + idRegistro));
  }

  private static String recortar(String texto) {
    if (texto == null || texto.length() <= LIMITE_TEXTO) {
      return texto;
    }
    return texto.substring(0, LIMITE_TEXTO);
  }
}
