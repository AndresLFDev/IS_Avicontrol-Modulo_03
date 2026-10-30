package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.GenerarLiquidacionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** Body de {@code POST /api/v1/liquidaciones/previa}. {@code usuario} llega recortado. */
public record VistaPreviaRequest(
    @NotNull(message = "El lote es obligatorio.") UUID idLote,
    @Positive(message = "El precio por kg debe ser mayor que 0") BigDecimal precioKgCop,
    @NotBlank(message = "El usuario responsable es obligatorio.")
        @Size(
            min = 3,
            max = 100,
            message = "El usuario responsable debe tener entre 3 y 100 caracteres.")
        String usuario) {

  public VistaPreviaRequest {
    usuario = usuario == null ? null : usuario.strip();
  }

  GenerarLiquidacionCommand toCommand() {
    return new GenerarLiquidacionCommand(idLote, precioKgCop, usuario, null);
  }
}
