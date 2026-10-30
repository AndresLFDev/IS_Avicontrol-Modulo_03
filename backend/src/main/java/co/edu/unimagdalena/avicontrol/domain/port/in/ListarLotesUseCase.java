package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LotePageDto;

/** M3-CU01 — Consultar la lista de lotes sin Liquidación {@code ACTIVA}. */
public interface ListarLotesUseCase {

  /**
   * @param etapa etapa listable o {@code null} para todas; {@link EtapaLote#LIQUIDADO} se rechaza
   * @param search texto libre: nombre de lote, nombre de galpón o UUID ({@code null} o vacío = sin
   *     búsqueda)
   * @param page índice de página (0-based)
   * @param size tamaño de página; se limita a 6 (FR-011)
   */
  LotePageDto listarLotes(EtapaLote etapa, String search, int page, int size);
}
