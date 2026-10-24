package co.edu.unimagdalena.avicontrol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Arranca la aplicación completa contra PostgreSQL y Kafka reales y verifica el esquema. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AvicontrolApplicationIT {

  private static final List<String> TABLAS =
      List.of(
          "registro_sincronizacion",
          "galpon",
          "lote",
          "alerta_vaciado_sanitario",
          "resultado_final_sacrificio",
          "aviso_utilizacion_resultado",
          "partida_alimento_lote",
          "consumo_medicamento_lote",
          "tramo_recepcion_consumo",
          "liquidacion",
          "partida_costo_lote",
          "snapshot_datos_origen",
          "registro_anulacion");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void aplicaLasMigracionesV1aV3() {
    var versiones =
        jdbc.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
            String.class);
    assertThat(versiones).containsExactly("1", "2", "3");
  }

  @Test
  void creaTodasLasTablasDelModelo() {
    var tablas =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String.class);
    assertThat(tablas).containsAll(TABLAS);
  }

  @Test
  void elPerfilTestNoCargaDatosDeDemostracion() {
    assertThat(jdbc.queryForObject("SELECT count(*) FROM galpon", Integer.class)).isZero();
  }

  @Test
  void elIndiceParcialPermiteSoloUnaLiquidacionActivaPorLote() {
    var indice =
        jdbc.queryForObject(
            "SELECT indexdef FROM pg_indexes WHERE indexname = 'uq_lote_activa'", String.class);
    assertThat(indice).contains("UNIQUE").contains("WHERE");
  }
}
