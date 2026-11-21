package co.edu.unimagdalena.avicontrol.application.service.sync;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.GalponM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.HistorialLotesM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteM1;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM1Repository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica a la copia local un galpón de M1 con sus lotes, en una sola transacción (CU07). Valida
 * antes de escribir (FR-010 y FR-011): un dato inválido se omite y se informa, sin sobrescribir lo
 * que ya había. Es un bean aparte de {@link SyncModulo1Service} para que la transacción abarque
 * solo la escritura de un galpón y no las llamadas HTTP a M1.
 */
@Service
public class AplicarGalponM1Service {

  /** Estados operativos del galpón (acuerdo de contratos de M1, sección 2.1). */
  static final Set<String> ESTADOS_GALPON =
      Set.of(
          "DISPONIBLE",
          "VACIADO_SANITARIO",
          "PRODUCTIVO",
          "EN_COSECHA",
          "MANTENIMIENTO",
          "AISLAMIENTO");

  static final String LOTE_ACTIVO = "ACTIVO";
  static final String LOTE_DESVINCULADO = "DESVINCULADO";

  private final CopiaLocalM1Repository copia;

  public AplicarGalponM1Service(CopiaLocalM1Repository copia) {
    this.copia = copia;
  }

  /** Resultado de aplicar un galpón: filas escritas y datos omitidos. */
  public record Aplicacion(int registros, List<String> incidencias) {}

  /**
   * @param detalle el galpón con su lote activo (de {@code GET /galpones/{id}})
   * @param historial lotes anteriores (de {@code GET /galpones/{id}/lotes})
   * @param idRegistro ejecución de la bitácora a la que se atribuyen las alertas nuevas
   */
  @Transactional
  public Aplicacion aplicar(
      GalponM1 detalle, HistorialLotesM1 historial, long idRegistro, Instant ahora) {
    List<String> incidencias = new ArrayList<>();
    String errorGalpon = validarGalpon(detalle);
    if (errorGalpon != null) {
      incidencias.add("Galpón " + detalle.id() + " omitido: " + errorGalpon);
      return new Aplicacion(0, incidencias);
    }

    int registros = 1;
    copia.guardarGalpon(
        detalle.id(), detalle.nombre(), detalle.capacidad().intValue(), detalle.estado(), ahora);

    // El detalle trae el lote activo; si no, el historial puede traerlo igualmente
    LoteM1 activo = detalle.loteActivo() != null ? detalle.loteActivo() : historial.loteActivo();
    if (activo != null) {
      registros += aplicarLote(detalle.id(), activo, LOTE_ACTIVO, ahora, incidencias);
    }

    for (LoteM1 anterior : historial.lotesAnteriores()) {
      int escritos = aplicarLote(detalle.id(), anterior, LOTE_DESVINCULADO, ahora, incidencias);
      registros += escritos;
      // Un lote desvinculado ya pasó por un vaciado sanitario aceptado por M1: eso es la alerta
      if (escritos > 0
          && copia.crearAlertaVaciadoSiNoExiste(
              idAlertaDerivada(anterior.id()), detalle.id(), anterior.id(), ahora, idRegistro)) {
        registros++;
      }
    }
    return new Aplicacion(registros, incidencias);
  }

  /** Id de la alerta derivada: el mismo lote siempre produce el mismo id. */
  static UUID idAlertaDerivada(UUID idLote) {
    return UUID.nameUUIDFromBytes(("alerta-vaciado:" + idLote).getBytes(StandardCharsets.UTF_8));
  }

  private int aplicarLote(
      UUID idGalpon, LoteM1 lote, String estado, Instant ahora, List<String> incidencias) {
    String error = validarLote(lote);
    if (error != null) {
      incidencias.add("Lote " + lote.id() + " omitido: " + error);
      return 0;
    }
    copia.guardarLote(
        lote.id(),
        idGalpon,
        lote.nombre(),
        lote.fechaIngreso(),
        lote.poblacionInicial().intValue(),
        lote.poblacionActual().intValue(),
        lote.costoTotal(),
        estado,
        ahora);
    return 1;
  }

  private static String validarGalpon(GalponM1 galpon) {
    if (galpon == null || galpon.id() == null) {
      return "sin id";
    }
    if (galpon.nombre() == null || galpon.nombre().isBlank()) {
      return "sin nombre";
    }
    if (galpon.capacidad() == null
        || galpon.capacidad() <= 0
        || galpon.capacidad() > Integer.MAX_VALUE) {
      return "capacidad inválida";
    }
    if (galpon.estado() == null || !ESTADOS_GALPON.contains(galpon.estado())) {
      return "estado fuera del catálogo: " + galpon.estado();
    }
    return null;
  }

  private static String validarLote(LoteM1 lote) {
    if (lote == null || lote.id() == null) {
      return "sin id";
    }
    if (lote.nombre() == null || lote.nombre().isBlank() || lote.fechaIngreso() == null) {
      return "sin nombre o fecha de ingreso";
    }
    if (lote.poblacionInicial() == null || lote.poblacionInicial() <= 0) {
      return "sin población inicial";
    }
    if (lote.poblacionActual() == null
        || lote.poblacionActual() < 0
        || lote.poblacionActual() > lote.poblacionInicial()) {
      return "población actual inválida";
    }
    if (lote.poblacionInicial() > Integer.MAX_VALUE) {
      return "población inicial fuera de rango";
    }
    if (lote.costoTotal() == null || lote.costoTotal() <= 0) {
      return "costo total no positivo";
    }
    return null;
  }
}
