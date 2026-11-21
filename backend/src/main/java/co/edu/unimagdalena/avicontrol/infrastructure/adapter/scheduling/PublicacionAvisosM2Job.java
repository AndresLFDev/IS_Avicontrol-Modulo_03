package co.edu.unimagdalena.avicontrol.infrastructure.adapter.scheduling;

import co.edu.unimagdalena.avicontrol.domain.port.in.PublicarAvisosUtilizacionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Envía a M2 los avisos de utilización pendientes cada {@code avisos.intervalo}. Sin {@code
 * M2_BASE_URL} no hace nada y los avisos quedan guardados. Nunca lanza: lo que falle sigue
 * pendiente y se reintenta en el siguiente ciclo.
 */
@Component
public class PublicacionAvisosM2Job {

  private static final Logger LOG = LoggerFactory.getLogger(PublicacionAvisosM2Job.class);

  private final PublicarAvisosUtilizacionUseCase publicar;
  private final String urlBase;

  public PublicacionAvisosM2Job(
      PublicarAvisosUtilizacionUseCase publicar, @Value("${m2.base-url:}") String urlBase) {
    this.publicar = publicar;
    this.urlBase = urlBase;
  }

  @Scheduled(
      initialDelayString = "${avisos.retraso-inicial:PT45S}",
      fixedDelayString = "${avisos.intervalo:PT1M}")
  public void ejecutar() {
    if (urlBase.isBlank()) {
      LOG.debug("M2_BASE_URL no está configurada: los avisos quedan pendientes");
      return;
    }
    try {
      var resumen = publicar.publicar();
      if (resumen.entregados() > 0) {
        LOG.info("Avisos de utilización confirmados por M2: {}", resumen.entregados());
      }
      if (resumen.fallidos() > 0) {
        LOG.warn(
            "Avisos de utilización pendientes: {} ({})",
            resumen.fallidos(),
            String.join("; ", resumen.errores()));
      }
    } catch (RuntimeException e) {
      LOG.error("Error inesperado al enviar avisos a M2", e);
    }
  }
}
