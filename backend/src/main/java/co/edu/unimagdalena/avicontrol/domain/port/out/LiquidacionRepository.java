package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaExistenteException;
import co.edu.unimagdalena.avicontrol.domain.model.CalculoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.DatosLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.Liquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialItemDto;
import co.edu.unimagdalena.avicontrol.domain.shared.Pagina;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de la Liquidación con sus partidas de costo y snapshots de origen (M3-CU03 y
 * M3-CU04).
 */
public interface LiquidacionRepository {

  /** Id de la Liquidación {@code ACTIVA} del lote, si existe. */
  Optional<Long> buscarIdActivaPorLote(UUID idLote);

  /**
   * Guarda la Liquidación {@code ACTIVA} y devuelve su id.
   *
   * @throws LiquidacionYaExistenteException si otra confirmación del mismo lote se adelantó
   */
  long guardarActiva(
      DatosLiquidacion datos, CalculoLiquidacion calculo, String usuario, Instant fechaHora);

  Optional<Liquidacion> buscarPorId(long idLiquidacion);

  /** Lee la Liquidación y bloquea su fila hasta el fin de la transacción (M3-CU04). */
  Optional<Liquidacion> bloquearParaAnular(long idLiquidacion);

  /** Marca {@code ANULADA} la Liquidación y crea su registro de auditoría. */
  RegistroAnulacion anular(long idLiquidacion, String motivo, String usuario, Instant fechaHora);

  /**
   * Historial de Liquidaciones {@code ACTIVA} y {@code ANULADA}, de la más reciente a la más
   * antigua (M3-CU06).
   *
   * @param search texto ya normalizado: nombre o UUID de galpón o de lote ({@code null} = todas)
   * @param desde instante inicial inclusivo ({@code null} = sin límite)
   * @param hastaExclusivo instante final exclusivo ({@code null} = sin límite)
   * @param page índice de página (0-based)
   * @param size tamaño de página
   */
  Pagina<HistorialItemDto> buscarHistorial(
      String search, Instant desde, Instant hastaExclusivo, int page, int size);
}
