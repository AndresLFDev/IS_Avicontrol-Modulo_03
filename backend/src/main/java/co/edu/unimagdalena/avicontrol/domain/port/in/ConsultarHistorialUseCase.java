package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.exception.RangoFechasInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialPageDto;
import java.time.LocalDate;

/** M3-CU06 — Consultar el historial de Liquidaciones, de la más reciente a la más antigua. */
public interface ConsultarHistorialUseCase {

  /**
   * @param search texto libre: nombre o UUID de galpón o de lote ({@code null} o vacío = todas)
   * @param desde fecha inicial inclusiva en la zona {@code America/Bogota} ({@code null} = sin
   *     límite)
   * @param hasta fecha final inclusiva en la zona {@code America/Bogota} ({@code null} = sin
   *     límite)
   * @param page índice de página (0-based)
   * @param size tamaño de página
   * @throws RangoFechasInvalidoException si {@code desde} es posterior a {@code hasta} (FR-005)
   */
  HistorialPageDto consultarHistorial(
      String search, LocalDate desde, LocalDate hasta, int page, int size);
}
