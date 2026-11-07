package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarHistorialUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialPageDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** M3-CU06 — {@code GET /api/v1/liquidaciones} (plan 004, contrato REST). */
@RestController
@RequestMapping("/api/v1/liquidaciones")
public class HistorialController {

  private final ConsultarHistorialUseCase consultarHistorial;

  public HistorialController(ConsultarHistorialUseCase consultarHistorial) {
    this.consultarHistorial = consultarHistorial;
  }

  /**
   * {@code GET /api/v1/liquidaciones?search=norte&desde=2026-07-01&hasta=2026-09-30&page=0&size=10}
   */
  @GetMapping
  public ResponseEntity<HistorialPageDto> consultarHistorial(
      @RequestParam(required = false) @Size(max = 100) String search,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
    return ResponseEntity.ok(
        consultarHistorial.consultarHistorial(search, desde, hasta, page, size));
  }
}
