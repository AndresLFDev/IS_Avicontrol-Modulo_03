package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.GalponM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.HistorialLotesM1;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de solo lectura al REST de Módulo 1 (acuerdo de contratos de M1): {@code GET
 * /galpones}, {@code GET /galpones/{id}} y {@code GET /galpones/{id}/lotes}. Plan 001, CU07.
 */
public interface ConsultarGalponesM1 {

  /**
   * Todas las páginas de la lista de galpones (tamaño fijo de 10, parámetro {@code page}).
   *
   * @throws co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM1Exception si M1 falla
   */
  List<GalponM1> listarGalpones();

  /** Detalle del galpón con su lote activo; vacío si M1 responde {@code 404}. */
  Optional<GalponM1> detalle(UUID idGalpon);

  /** Lote activo y lotes anteriores del galpón; vacío si M1 responde {@code 404}. */
  Optional<HistorialLotesM1> historial(UUID idGalpon);
}
