package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LoteJpaRepository extends JpaRepository<LoteEntity, UUID> {

  /**
   * Etapa del lote en el orden de M3-CU01.FR-013. Los que no cumplen ninguna regla quedan con etapa
   * NULL y no se listan.
   */
  String ETAPA =
      "CASE"
          + " WHEN a.idAlerta IS NOT NULL AND liq.id IS NOT NULL THEN 'LIQUIDADO'"
          + " WHEN a.idAlerta IS NOT NULL THEN 'POR_LIQUIDAR'"
          + " WHEN g.estado = 'PRODUCTIVO' THEN 'PRODUCTIVO'"
          + " WHEN g.estado = 'EN_COSECHA' THEN 'EN_COSECHA'"
          + " WHEN g.estado = 'AISLAMIENTO' THEN 'AISLAMIENTO'"
          + " END";

  /**
   * Lote con su galpón, la alerta de vaciado, la Liquidación ACTIVA y el resultado de sacrificio.
   */
  String UNIONES =
      "FROM LoteEntity l"
          + " JOIN l.galpon g"
          + " LEFT JOIN AlertaVaciadoSanitarioEntity a ON a.idLote = l.idLote"
          + " LEFT JOIN LiquidacionEntity liq ON liq.idLote = l.idLote"
          + " AND liq.estado = co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion.ACTIVA"
          + " LEFT JOIN ResultadoFinalSacrificioEntity r ON r.idLote = l.idLote";

  /** Solo lotes listables (FR-014), con filtro opcional por etapa y búsqueda. */
  String FILTRO =
      " WHERE ("
          + ETAPA
          + ") IS NOT NULL"
          + " AND ("
          + ETAPA
          + ") <> 'LIQUIDADO'"
          + " AND (cast(:etapa as String) IS NULL OR ("
          + ETAPA
          + ") = cast(:etapa as String))"
          + " AND (cast(:search as String) IS NULL"
          + " OR l.nombre ilike cast(:patron as String) escape '!'"
          + " OR g.nombre ilike cast(:patron as String) escape '!'"
          + " OR cast(l.idLote as String) = lower(cast(:search as String))"
          + " OR cast(g.idGalpon as String) = lower(cast(:search as String)))";

  @Query(
      value =
          "SELECT new co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence"
              + ".LoteFilaListado(l.idLote, l.nombre, l.fechaIngreso, g.idGalpon, g.nombre, "
              + ETAPA
              + ", l.poblacionActual,"
              + " CASE WHEN r.idResultado IS NOT NULL THEN true ELSE false END) "
              + UNIONES
              + FILTRO
              + " ORDER BY l.fechaIngreso, l.nombre, l.idLote",
      countQuery = "SELECT COUNT(l) " + UNIONES + FILTRO)
  Page<LoteFilaListado> buscarListables(
      String etapa, String search, String patron, Pageable paginacion);

  @Query(
      "SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END "
          + UNIONES
          + " WHERE ("
          + ETAPA
          + ") IS NOT NULL AND ("
          + ETAPA
          + ") <> 'LIQUIDADO'")
  boolean existeAlgunoListable();

  /** Solo el nombre: evita leer campos de un proxy perezoso ya presente en la sesión. */
  @Query("SELECT l.nombre FROM LoteEntity l WHERE l.idLote = :idLote")
  Optional<String> findNombre(UUID idLote);

  /** El lote con su galpón en una sola consulta. */
  @Query("SELECT l FROM LoteEntity l JOIN FETCH l.galpon WHERE l.idLote = :idLote")
  Optional<LoteEntity> findConGalpon(UUID idLote);
}
