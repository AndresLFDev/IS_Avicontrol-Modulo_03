package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;

/** M3-CU05: desglose de una Liquidación, ACTIVA o ANULADA. */
public interface ConsultarDesgloseUseCase {

  DesgloseDto consultarDesglose(long idLiquidacion);
}
