package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.AnulacionDto;

/** M3-CU04: anular una Liquidación ACTIVA dejando su registro de auditoría. */
public interface AnularLiquidacionUseCase {

  AnulacionDto anularLiquidacion(long idLiquidacion, String motivo, String usuario);
}
