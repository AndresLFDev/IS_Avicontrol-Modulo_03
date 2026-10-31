package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.GenerarLiquidacionCommand;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionDto;

/** CU03, paso 2 confirmado: persistir la Liquidación. */
public interface GenerarLiquidacionUseCase {

  LiquidacionDto generarLiquidacion(GenerarLiquidacionCommand comando);
}
