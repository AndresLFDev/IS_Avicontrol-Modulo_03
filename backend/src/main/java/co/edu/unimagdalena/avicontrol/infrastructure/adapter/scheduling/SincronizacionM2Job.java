package co.edu.unimagdalena.avicontrol.infrastructure.adapter.scheduling;

import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo2UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara la consulta a M2 poco después de arrancar (cuando la de M1 ya dejó los lotes) y luego
 * cada {@code sync.m2.intervalo}. Sin {@code M2_BASE_URL} no hace nada (desarrollo y pruebas), y
 * nunca lanza: un fallo ya queda en la bitácora y el siguiente ciclo reintenta.
 */
@Component
public class SincronizacionM2Job {

  private static final Logger LOG = LoggerFactory.getLogger(SincronizacionM2Job.class);

  private final SincronizarModulo2UseCase sincronizar;
  private final String urlBase;

  public SincronizacionM2Job(
      SincronizarModulo2UseCase sincronizar, @Value("${m2.base-url:}") String urlBase) {
    this.sincronizar = sincronizar;
    this.urlBase = urlBase;
  }

  @Scheduled(
      initialDelayString = "${sync.m2.retraso-inicial:PT30S}",
      fixedDelayString = "${sync.m2.intervalo:PT5M}")
  public void ejecutar() {
    if (urlBase.isBlank()) {
      LOG.debug("M2_BASE_URL no está configurada: no se sincroniza con Módulo 2");
      return;
    }
    try {
      var resumen = sincronizar.sincronizar();
      if (resumen.exitosa()) {
        LOG.info(
            "Sincronización con M2: {} registros, {} datos omitidos",
            resumen.registrosActualizados(),
            resumen.incidencias().size());
      } else {
        LOG.warn("Sincronización con M2 fallida: {}", resumen.error());
      }
    } catch (RuntimeException e) {
      LOG.error("Error inesperado al sincronizar con M2", e);
    }
  }
}
