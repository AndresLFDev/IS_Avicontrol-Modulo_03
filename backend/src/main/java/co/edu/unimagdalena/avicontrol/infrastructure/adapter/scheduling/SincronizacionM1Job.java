package co.edu.unimagdalena.avicontrol.infrastructure.adapter.scheduling;

import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo1UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara la sincronización con M1 poco después de arrancar y luego cada {@code sync.m1.intervalo}.
 * Sin {@code M1_BASE_URL} no hace nada (desarrollo y pruebas), y nunca lanza: un fallo ya queda en
 * la bitácora y el siguiente ciclo reintenta.
 */
@Component
public class SincronizacionM1Job {

  private static final Logger LOG = LoggerFactory.getLogger(SincronizacionM1Job.class);

  private final SincronizarModulo1UseCase sincronizar;
  private final String urlBase;

  public SincronizacionM1Job(
      SincronizarModulo1UseCase sincronizar, @Value("${m1.base-url:}") String urlBase) {
    this.sincronizar = sincronizar;
    this.urlBase = urlBase;
  }

  @Scheduled(
      initialDelayString = "${sync.m1.retraso-inicial:PT10S}",
      fixedDelayString = "${sync.m1.intervalo:PT5M}")
  public void ejecutar() {
    if (urlBase.isBlank()) {
      LOG.debug("M1_BASE_URL no está configurada: no se sincroniza con Módulo 1");
      return;
    }
    try {
      var resumen = sincronizar.sincronizar();
      if (resumen.exitosa()) {
        LOG.info(
            "Sincronización con M1: {} registros, {} datos omitidos",
            resumen.registrosActualizados(),
            resumen.incidencias().size());
      } else {
        LOG.warn("Sincronización con M1 fallida: {}", resumen.error());
      }
    } catch (RuntimeException e) {
      LOG.error("Error inesperado al sincronizar con M1", e);
    }
  }
}
