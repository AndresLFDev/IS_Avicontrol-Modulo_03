package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM2Exception;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.PartidaAlimentoM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.PartidaMedicamentoM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResultadoM2;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas de solo lectura a Módulo 2 por lote. Todas lanzan {@link ConsultaM2Exception}. */
public interface ConsultarFinanzasM2 {

  /** Resultado final del lote; vacío si M2 aún no lo registró ({@code 404}). */
  Optional<ResultadoM2> resultado(UUID idLote);

  /** Suministros de alimento del lote; lista vacía si no hay. */
  List<PartidaAlimentoM2> alimentos(UUID idLote);

  /** Consumos de medicamento del lote, un elemento por recepción; lista vacía si no hay. */
  List<PartidaMedicamentoM2> medicamentos(UUID idLote);
}
