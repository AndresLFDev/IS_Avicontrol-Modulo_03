package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.GenerarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.PreviewLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionPreviaDto;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** M3-CU03 — generación en dos pasos y consulta de la Liquidación (plan 003). */
@RestController
@RequestMapping("/api/v1/liquidaciones")
public class LiquidacionController {

  private final PreviewLiquidacionUseCase vistaPrevia;
  private final GenerarLiquidacionUseCase generar;
  private final ConsultarLiquidacionUseCase consultar;

  public LiquidacionController(
      PreviewLiquidacionUseCase vistaPrevia,
      GenerarLiquidacionUseCase generar,
      ConsultarLiquidacionUseCase consultar) {
    this.vistaPrevia = vistaPrevia;
    this.generar = generar;
    this.consultar = consultar;
  }

  /** Pasos 1 y 2: vista previa sin persistir (200, no 201). */
  @PostMapping("/previa")
  public ResponseEntity<LiquidacionPreviaDto> vistaPrevia(
      @Valid @RequestBody VistaPreviaRequest request) {
    return ResponseEntity.ok(vistaPrevia.calcularVistaPrevia(request.toCommand()));
  }

  /** Paso 2 confirmado: persiste la Liquidación ACTIVA. */
  @PostMapping
  public ResponseEntity<LiquidacionDto> generar(
      @Valid @RequestBody GenerarLiquidacionRequest request) {
    var liquidacion = generar.generarLiquidacion(request.toCommand());
    return ResponseEntity.created(
            URI.create("/api/v1/liquidaciones/" + liquidacion.idLiquidacion()))
        .body(liquidacion);
  }

  /** FR-014: Liquidación existente, ACTIVA o ANULADA. */
  @GetMapping("/{id}")
  public ResponseEntity<LiquidacionDto> consultar(@PathVariable long id) {
    return ResponseEntity.ok(consultar.consultarLiquidacion(id));
  }
}
