package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionDto;

/** CU03.FR-014: consultar una Liquidación existente, ACTIVA o ANULADA. */
public interface ConsultarLiquidacionUseCase {

  LiquidacionDto consultarLiquidacion(long idLiquidacion);
}
