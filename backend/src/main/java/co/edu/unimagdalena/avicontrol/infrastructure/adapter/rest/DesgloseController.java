package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarDesgloseUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.ExportarExcelUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** M3-CU05: desglose de una Liquidación y su exportación a Excel (plan 004, contrato REST). */
@RestController
@RequestMapping("/api/v1/liquidaciones")
public class DesgloseController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final ConsultarDesgloseUseCase consultarDesglose;
  private final ExportarExcelUseCase exportarExcel;

  public DesgloseController(
      ConsultarDesgloseUseCase consultarDesglose, ExportarExcelUseCase exportarExcel) {
    this.consultarDesglose = consultarDesglose;
    this.exportarExcel = exportarExcel;
  }

  @GetMapping("/{id}/desglose")
  public ResponseEntity<DesgloseDto> consultarDesglose(@PathVariable long id) {
    return ResponseEntity.ok(consultarDesglose.consultarDesglose(id));
  }

  @GetMapping("/{id}/desglose/excel")
  public ResponseEntity<byte[]> descargarExcel(@PathVariable long id) {
    byte[] archivo = exportarExcel.generarReporteExcelLiquidacion(id);
    var adjunto =
        ContentDisposition.attachment().filename("Liquidacion_Lote_" + id + ".xlsx").build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, adjunto.toString())
        .contentType(XLSX)
        .body(archivo);
  }
}
