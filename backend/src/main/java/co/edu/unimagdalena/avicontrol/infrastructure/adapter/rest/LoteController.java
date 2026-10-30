package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.port.in.ListarLotesUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LotePageDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** M3-CU01 — {@code GET /api/v1/lotes} (plan 002, contrato REST). */
@RestController
@RequestMapping("/api/v1/lotes")
public class LoteController {

  private final ListarLotesUseCase listarLotesUseCase;

  public LoteController(ListarLotesUseCase listarLotesUseCase) {
    this.listarLotesUseCase = listarLotesUseCase;
  }

  /** {@code GET /api/v1/lotes?etapa=POR_LIQUIDAR&search=norte&page=0&size=6} */
  @GetMapping
  public ResponseEntity<LotePageDto> listarLotes(
      @RequestParam(required = false) EtapaLote etapa, // valor desconocido → 400
      @RequestParam(required = false) @Size(max = 100) String search,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "6") @Min(1) int size) {
    return ResponseEntity.ok(listarLotesUseCase.listarLotes(etapa, search, page, size));
  }
}
