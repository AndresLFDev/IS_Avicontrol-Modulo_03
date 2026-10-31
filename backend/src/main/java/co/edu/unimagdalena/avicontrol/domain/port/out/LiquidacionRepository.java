package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaExistenteException;
import co.edu.unimagdalena.avicontrol.domain.model.CalculoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.DatosLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.Liquidacion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de la Liquidación con sus partidas de costo y snapshots de origen (M3-CU03). */
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
}
