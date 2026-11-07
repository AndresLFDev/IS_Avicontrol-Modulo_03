package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.exception.ExportacionNoPermitidaException;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarDesgloseUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.ExportarExcelUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.out.GeneradorExcelPort;
import org.springframework.stereotype.Service;

/**
 * M3-CU05: el Excel parte del mismo desglose de la pantalla. Sin transacción propia: la conexión se
 * libera al terminar la consulta y no queda retenida mientras se escribe el libro.
 */
@Service
public class ExportarExcelService implements ExportarExcelUseCase {

  private final ConsultarDesgloseUseCase consultarDesglose;
  private final GeneradorExcelPort generadorExcel;

  public ExportarExcelService(
      ConsultarDesgloseUseCase consultarDesglose, GeneradorExcelPort generadorExcel) {
    this.consultarDesglose = consultarDesglose;
    this.generadorExcel = generadorExcel;
  }

  @Override
  public byte[] generarReporteExcelLiquidacion(long idLiquidacion) {
    var desglose = consultarDesglose.consultarDesglose(idLiquidacion);
    if (!desglose.exportacionPermitida()) {
      throw new ExportacionNoPermitidaException(idLiquidacion);
    }
    return generadorExcel.generar(desglose);
  }
}
