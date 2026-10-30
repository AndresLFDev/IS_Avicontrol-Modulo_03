package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarEstadoSincronizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RT-09 — {@code GET /api/v1/sincronizacion/estado} (plan 001, T017). */
@RestController
@RequestMapping("/api/v1/sincronizacion")
public class SincronizacionController {

  private final ConsultarEstadoSincronizacionUseCase estadoSincronizacion;

  public SincronizacionController(ConsultarEstadoSincronizacionUseCase estadoSincronizacion) {
    this.estadoSincronizacion = estadoSincronizacion;
  }

  @GetMapping("/estado")
  public ResponseEntity<EstadoSincronizacionDto> estado() {
    return ResponseEntity.ok(estadoSincronizacion.consultar());
  }
}
