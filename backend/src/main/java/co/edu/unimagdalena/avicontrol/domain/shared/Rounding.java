package co.edu.unimagdalena.avicontrol.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Reglas transversales de redondeo: RT-01 (montos COP enteros) y RT-02 (porcentajes). */
public final class Rounding {

  private static final BigDecimal CIEN = BigDecimal.valueOf(100);

  private Rounding() {}

  /** RT-01: cifra monetaria total como entero COP con {@code HALF_UP}. */
  public static long cop(BigDecimal valor) {
    return valor.setScale(0, RoundingMode.HALF_UP).longValueExact();
  }

  /** RT-02: {@code parte / total × 100} con 2 decimales y {@code HALF_UP}. */
  public static BigDecimal porcentaje(long parte, long total) {
    if (total == 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return BigDecimal.valueOf(parte)
        .multiply(CIEN)
        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
  }
}
