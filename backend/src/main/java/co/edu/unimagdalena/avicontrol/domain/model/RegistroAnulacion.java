package co.edu.unimagdalena.avicontrol.domain.model;

import java.time.Instant;

/** Registro de la anulación de una Liquidación (M3-CU04). */
public record RegistroAnulacion(
    Long idAnulacion, String motivo, Instant fechaHoraAnulacion, String usuarioResponsable) {}
