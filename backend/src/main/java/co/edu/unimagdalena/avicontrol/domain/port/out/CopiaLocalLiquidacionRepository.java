package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.model.LoteLiquidable;
import co.edu.unimagdalena.avicontrol.domain.model.PartidaAlimento;
import co.edu.unimagdalena.avicontrol.domain.model.ResultadoSacrificio;
import co.edu.unimagdalena.avicontrol.domain.model.TramoMedicamento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lectura de la copia local de Módulo 1 y Módulo 2 que alimenta la Liquidación (M3-CU03). */
public interface CopiaLocalLiquidacionRepository {

  Optional<LoteLiquidable> buscarLote(UUID idLote);

  /** Si Módulo 1 ya emitió la alerta de vaciado sanitario del lote. */
  boolean tieneAlertaVaciado(UUID idLote);

  /** Resultado final de sacrificio de Módulo 2; vacío si aún no llega (o siniestro total). */
  Optional<ResultadoSacrificio> buscarResultadoSacrificio(UUID idLote);

  List<PartidaAlimento> buscarPartidasAlimento(UUID idLote);

  List<TramoMedicamento> buscarTramosMedicamento(UUID idLote);
}
