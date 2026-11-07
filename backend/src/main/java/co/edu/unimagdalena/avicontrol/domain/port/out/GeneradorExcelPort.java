package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.exception.ExportacionFallidaException;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;

/** Construye el libro {@code .xlsx}; así la aplicación no depende de Apache POI. */
public interface GeneradorExcelPort {

  /**
   * @throws ExportacionFallidaException si el libro no se puede escribir
   */
  byte[] generar(DesgloseDto desglose);
}
