package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.model.LoteListado;
import co.edu.unimagdalena.avicontrol.domain.port.out.LoteRepository;
import co.edu.unimagdalena.avicontrol.domain.shared.Pagina;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lista de lotes con la etapa derivada en SQL (plan 002): un {@code CASE} sobre la alerta de
 * vaciado sanitario, la Liquidación {@code ACTIVA} y el estado del galpón, en el orden de
 * M3-CU01.FR-013. Así el filtro por etapa, la exclusión de {@code LIQUIDADO} y el total de la
 * paginación se resuelven en la base de datos.
 */
@Repository
public class LoteJdbcAdapter implements LoteRepository {

  /** Lotes con su etapa derivada. Los que no cumplen ninguna regla quedan con etapa NULL. */
  private static final String LOTES_CON_ETAPA =
      """
      WITH lote_etapa AS (
        SELECT l.id_lote,
               l.nombre AS nombre_lote,
               l.fecha_ingreso,
               g.id_galpon,
               g.nombre AS nombre_galpon,
               l.poblacion_actual,
               r.id_resultado IS NOT NULL AS tiene_resultado,
               CASE
                 WHEN a.id_alerta IS NOT NULL AND liq.id_liquidacion IS NOT NULL THEN 'LIQUIDADO'
                 WHEN a.id_alerta IS NOT NULL THEN 'POR_LIQUIDAR'
                 WHEN g.estado = 'PRODUCTIVO' THEN 'PRODUCTIVO'
                 WHEN g.estado = 'EN_COSECHA' THEN 'EN_COSECHA'
                 WHEN g.estado = 'AISLAMIENTO' THEN 'AISLAMIENTO'
               END AS etapa
          FROM lote l
          JOIN galpon g ON g.id_galpon = l.id_galpon
          LEFT JOIN alerta_vaciado_sanitario a ON a.id_lote = l.id_lote
          LEFT JOIN liquidacion liq ON liq.id_lote = l.id_lote AND liq.estado = 'ACTIVA'
          LEFT JOIN resultado_final_sacrificio r ON r.id_lote = l.id_lote
      )
      """;

  /** Solo lotes listables (FR-014), con filtro opcional por etapa y búsqueda. */
  private static final String FILTRO =
      """
      WHERE etapa IS NOT NULL
        AND etapa <> 'LIQUIDADO'
        AND (CAST(:etapa AS VARCHAR) IS NULL OR etapa = CAST(:etapa AS VARCHAR))
        AND (CAST(:search AS VARCHAR) IS NULL
             OR nombre_lote ILIKE CAST(:patron AS VARCHAR) ESCAPE '!'
             OR nombre_galpon ILIKE CAST(:patron AS VARCHAR) ESCAPE '!'
             OR CAST(id_lote AS VARCHAR) = LOWER(CAST(:search AS VARCHAR))
             OR CAST(id_galpon AS VARCHAR) = LOWER(CAST(:search AS VARCHAR)))
      """;

  private final JdbcClient jdbc;

  public LoteJdbcAdapter(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Pagina<LoteListado> buscarListables(EtapaLote etapa, String search, int page, int size) {
    String codigoEtapa = etapa == null ? null : etapa.name();
    String patron = search == null ? null : "%" + escaparLike(search) + "%";

    long total =
        jdbc.sql(LOTES_CON_ETAPA + "SELECT COUNT(*) FROM lote_etapa " + FILTRO)
            .param("etapa", codigoEtapa)
            .param("search", search)
            .param("patron", patron)
            .query(Long.class)
            .single();

    var filas =
        jdbc.sql(
                LOTES_CON_ETAPA
                    + "SELECT * FROM lote_etapa "
                    + FILTRO
                    + " ORDER BY fecha_ingreso, nombre_lote, id_lote LIMIT :limite OFFSET :desde")
            .param("etapa", codigoEtapa)
            .param("search", search)
            .param("patron", patron)
            .param("limite", size)
            .param("desde", (long) page * size)
            .query(LoteJdbcAdapter::aLote)
            .list();

    return Pagina.de(filas, page, size, total);
  }

  @Override
  public boolean existeAlgunoListable() {
    return jdbc.sql(
            LOTES_CON_ETAPA
                + "SELECT EXISTS (SELECT 1 FROM lote_etapa"
                + " WHERE etapa IS NOT NULL AND etapa <> 'LIQUIDADO')")
        .query(Boolean.class)
        .single();
  }

  private static LoteListado aLote(ResultSet rs, int fila) throws SQLException {
    return new LoteListado(
        rs.getObject("id_lote", UUID.class),
        rs.getString("nombre_lote"),
        rs.getObject("fecha_ingreso", LocalDate.class),
        rs.getObject("id_galpon", UUID.class),
        rs.getString("nombre_galpon"),
        EtapaLote.valueOf(rs.getString("etapa")),
        rs.getInt("poblacion_actual"),
        rs.getBoolean("tiene_resultado"));
  }

  /** Escapa los comodines de LIKE (con {@code !} como carácter de escape) para buscar literal. */
  static String escaparLike(String texto) {
    return texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }
}
