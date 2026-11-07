package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de {@code PUT /api/v1/liquidaciones/{id}/anulacion}. */
public record AnulacionRequest(
    @NotBlank(message = "El motivo de anulación es obligatorio.")
        @Size(
            min = 10,
            max = 500,
            message = "El motivo de anulación debe tener entre 10 y 500 caracteres.")
        String motivo,
    @NotBlank(message = "El usuario responsable es obligatorio.")
        @Size(
            min = 3,
            max = 100,
            message = "El usuario responsable debe tener entre 3 y 100 caracteres.")
        String usuario) {

  public AnulacionRequest {
    motivo = motivo == null ? null : motivo.strip();
    usuario = usuario == null ? null : usuario.strip();
  }
}
