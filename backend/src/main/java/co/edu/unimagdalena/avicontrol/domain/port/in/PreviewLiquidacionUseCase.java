package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.GenerarLiquidacionCommand;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionPreviaDto;

/** CU03, pasos 1 y 2: calcular la vista previa sin persistir. */
public interface PreviewLiquidacionUseCase {

  LiquidacionPreviaDto calcularVistaPrevia(GenerarLiquidacionCommand comando);
}
