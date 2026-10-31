package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.model.LoteListado;
import co.edu.unimagdalena.avicontrol.domain.shared.Pagina;

/** Lectura de lotes con su etapa derivada (M3-CU01, plan 002). */
public interface LoteRepository {

  /**
   * Lotes listables (FR-014): con etapa derivada y sin Liquidación {@code ACTIVA}.
   *
   * @param etapa etapa a filtrar o {@code null} para todas
   * @param search texto ya normalizado: nombre de lote, nombre de galpón o UUID ({@code null} = sin
   *     búsqueda)
   * @param page índice de página (0-based)
   * @param size tamaño de página
   */
  Pagina<LoteListado> buscarListables(EtapaLote etapa, String search, int page, int size);

  /** Si existe al menos un lote listable, sin filtros; distingue FR-006 de FR-010. */
  boolean existeAlgunoListable();
}
