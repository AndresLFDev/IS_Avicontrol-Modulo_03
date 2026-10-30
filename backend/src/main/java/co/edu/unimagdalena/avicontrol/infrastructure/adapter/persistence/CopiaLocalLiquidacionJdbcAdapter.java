package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.LoteLiquidable;
import co.edu.unimagdalena.avicontrol.domain.model.PartidaAlimento;
import co.edu.unimagdalena.avicontrol.domain.model.ResultadoSacrificio;
import co.edu.unimagdalena.avicontrol.domain.model.TramoMedicamento;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalLiquidacionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Lee la copia local de M1 y M2 (tablas de V1 y V2) para la Liquidación. */
@Repository
public class CopiaLocalLiquidacionJdbcAdapter implements CopiaLocalLiquidacionRepository {

  private final JdbcClient jdbc;

  public CopiaLocalLiquidacionJdbcAdapter(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<LoteLiquidable> buscarLote(UUID idLote) {
    return jdbc.sql(
            """
            SELECT l.id_lote, l.nombre AS nombre_lote, g.id_galpon, g.nombre AS nombre_galpon,
                   l.poblacion_inicial, l.poblacion_actual, l.costo_total_cop, l.fecha_hora_sync
              FROM lote l
              JOIN galpon g ON g.id_galpon = l.id_galpon
             WHERE l.id_lote = :idLote
            """)
        .param("idLote", idLote)
        .query(
            (rs, i) ->
                new LoteLiquidable(
                    rs.getObject("id_lote", UUID.class),
                    rs.getString("nombre_lote"),
                    rs.getObject("id_galpon", UUID.class),
                    rs.getString("nombre_galpon"),
                    rs.getInt("poblacion_inicial"),
                    rs.getInt("poblacion_actual"),
                    rs.getLong("costo_total_cop"),
                    instante(rs, "fecha_hora_sync")))
        .optional();
  }

  @Override
  public boolean tieneAlertaVaciado(UUID idLote) {
    return jdbc.sql(
            "SELECT EXISTS (SELECT 1 FROM alerta_vaciado_sanitario WHERE id_lote = :idLote)")
        .param("idLote", idLote)
        .query(Boolean.class)
        .single();
  }

  @Override
  public Optional<ResultadoSacrificio> buscarResultadoSacrificio(UUID idLote) {
    return jdbc.sql(
            """
            SELECT id_resultado, cantidad_final_pollos, peso_total_kg, fecha_hora_sync
              FROM resultado_final_sacrificio
             WHERE id_lote = :idLote
            """)
        .param("idLote", idLote)
        .query(
            (rs, i) ->
                new ResultadoSacrificio(
                    rs.getObject("id_resultado", UUID.class),
                    rs.getInt("cantidad_final_pollos"),
                    rs.getBigDecimal("peso_total_kg"),
                    instante(rs, "fecha_hora_sync")))
        .optional();
  }

  @Override
  public List<PartidaAlimento> buscarPartidasAlimento(UUID idLote) {
    return jdbc.sql(
            """
            SELECT id_partida_origen, tipo_alimento, cantidad_kg, precio_unitario_kg_cop,
                   estado_valorizacion, referencia_origen, fecha_hora_sync
              FROM partida_alimento_lote
             WHERE id_lote = :idLote
             ORDER BY id_partida_origen
            """)
        .param("idLote", idLote)
        .query(
            (rs, i) ->
                new PartidaAlimento(
                    rs.getObject("id_partida_origen", UUID.class),
                    rs.getString("tipo_alimento"),
                    rs.getBigDecimal("cantidad_kg"),
                    rs.getBigDecimal("precio_unitario_kg_cop"),
                    pendiente(rs, "precio_unitario_kg_cop"),
                    rs.getString("referencia_origen"),
                    instante(rs, "fecha_hora_sync")))
        .list();
  }

  @Override
  public List<TramoMedicamento> buscarTramosMedicamento(UUID idLote) {
    return jdbc.sql(
            """
            SELECT c.id_consumo_origen, c.medicamento, t.referencia_recepcion,
                   t.cantidad_unidad_base, c.unidad_base, t.precio_unitario_base_cop,
                   t.estado_valorizacion, c.fecha_hora_sync
              FROM consumo_medicamento_lote c
              JOIN tramo_recepcion_consumo t ON t.id_consumo_origen = c.id_consumo_origen
             WHERE c.id_lote = :idLote
             ORDER BY c.id_consumo_origen, t.referencia_recepcion
            """)
        .param("idLote", idLote)
        .query(
            (rs, i) ->
                new TramoMedicamento(
                    rs.getObject("id_consumo_origen", UUID.class),
                    rs.getString("medicamento"),
                    rs.getString("referencia_recepcion"),
                    rs.getBigDecimal("cantidad_unidad_base"),
                    rs.getString("unidad_base"),
                    rs.getBigDecimal("precio_unitario_base_cop"),
                    pendiente(rs, "precio_unitario_base_cop"),
                    instante(rs, "fecha_hora_sync")))
        .list();
  }

  /** Sin precio también cuenta como pendiente, aunque el estado diga VALORIZADA. */
  private static boolean pendiente(ResultSet rs, String columnaPrecio) throws SQLException {
    return "PENDIENTE_PRECIO".equals(rs.getString("estado_valorizacion"))
        || rs.getBigDecimal(columnaPrecio) == null;
  }

  private static Instant instante(ResultSet rs, String columna) throws SQLException {
    var valor = rs.getObject(columna, OffsetDateTime.class);
    return valor == null ? null : valor.toInstant();
  }
}
