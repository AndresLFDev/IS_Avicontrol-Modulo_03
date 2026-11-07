package co.edu.unimagdalena.avicontrol.domain.port.in;

/** M3-CU05: exportación a Excel del desglose de una Liquidación {@code ACTIVA}. */
public interface ExportarExcelUseCase {

  byte[] generarReporteExcelLiquidacion(long idLiquidacion);
}
