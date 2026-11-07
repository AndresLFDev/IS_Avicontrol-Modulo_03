package co.edu.unimagdalena.avicontrol.domain.exception;

import java.time.LocalDate;

/** {@code 400 rango-fechas-invalido}: {@code desde} es posterior a {@code hasta} (CU06.FR-005). */
public class RangoFechasInvalidoException extends RuntimeException {

  public RangoFechasInvalidoException(LocalDate desde, LocalDate hasta) {
    super(
        "La fecha inicial ("
            + desde
            + ") debe ser menor o igual a la fecha final ("
            + hasta
            + ").");
  }
}
