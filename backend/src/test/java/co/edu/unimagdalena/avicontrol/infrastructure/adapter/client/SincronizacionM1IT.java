package co.edu.unimagdalena.avicontrol.infrastructure.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unimagdalena.avicontrol.TestcontainersConfiguration;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarEstadoSincronizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo1UseCase;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * CU07 de punta a punta (plan 001, T009): cliente REST real contra un M1 simulado, y PostgreSQL
 * real. Verifica lo que ve el usuario: los lotes de M1 aparecen y el desvinculado queda por
 * liquidar; repetir no duplica; si M1 cae, la copia se conserva y RT-09 lo advierte.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SincronizacionM1IT {

  private static final M1Simulado M1;

  static {
    try {
      M1 = new M1Simulado();
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registro) {
    registro.add("m1.base-url", M1::urlBase);
  }

  @AfterAll
  static void detener() {
    M1.close();
  }

  @Autowired private SincronizarModulo1UseCase sincronizar;
  @Autowired private ConsultarEstadoSincronizacionUseCase estado;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE alerta_vaciado_sanitario, lote, galpon, registro_sincronizacion RESTART IDENTITY"
            + " CASCADE");
    M1.limpiar();
    M1.conLosDatosDeLaDemo();
  }

  @Test
  void llenaLaCopiaLocalConLosGalponesLosLotesYLaAlertaDerivada() {
    var resumen = sincronizar.sincronizar();

    assertThat(resumen.exitosa()).isTrue();
    assertThat(resumen.incidencias()).isEmpty();
    assertThat(cuenta("galpon")).isEqualTo(2);
    assertThat(cuenta("lote")).isEqualTo(2);
    assertThat(cuenta("alerta_vaciado_sanitario")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT estado FROM lote WHERE id_lote = ?::uuid",
                String.class,
                M1Simulado.LOTE_ANTERIOR))
        .isEqualTo("DESVINCULADO");
    assertThat(
            jdbc.queryForObject(
                "SELECT estado FROM lote WHERE id_lote = ?::uuid",
                String.class,
                M1Simulado.LOTE_ACTIVO))
        .isEqualTo("ACTIVO");
    assertThat(
            jdbc.queryForObject(
                "SELECT aforo_maximo FROM galpon WHERE id_galpon = ?::uuid",
                Integer.class,
                M1Simulado.GALPON_UNO))
        .isEqualTo(5000);
    // La alerta apunta al lote desvinculado y a esta ejecución de la bitácora
    assertThat(
            jdbc.queryForObject("SELECT id_lote::text FROM alerta_vaciado_sanitario", String.class))
        .isEqualTo(M1Simulado.LOTE_ANTERIOR);
    assertThat(
            jdbc.queryForObject(
                "SELECT id_registro_sincronizacion FROM alerta_vaciado_sanitario", Long.class))
        .isEqualTo(jdbc.queryForObject("SELECT id FROM registro_sincronizacion", Long.class));
  }

  @Test
  void elLoteDesvinculadoApareceComoPorLiquidarEnLaListaDeLotes() {
    sincronizar.sincronizar();

    // Misma lógica de etapa que la lista (CU01.FR-013): alerta sin liquidación = por liquidar
    var etapas =
        jdbc.queryForList(
            "SELECT l.nombre, CASE WHEN a.id_alerta IS NOT NULL THEN 'POR_LIQUIDAR'"
                + " WHEN g.estado = 'AISLAMIENTO' THEN 'AISLAMIENTO' END AS etapa"
                + " FROM lote l JOIN galpon g ON g.id_galpon = l.id_galpon"
                + " LEFT JOIN alerta_vaciado_sanitario a ON a.id_lote = l.id_lote ORDER BY l.nombre");
    assertThat(etapas)
        .extracting(f -> f.get("nombre") + ":" + f.get("etapa"))
        .containsExactly("lote demo:POR_LIQUIDAR", "lote demo 3:AISLAMIENTO");
  }

  @Test
  void repetirLaSincronizacionNoDuplicaNiCambiaNada() {
    sincronizar.sincronizar();
    String alertaAntes =
        jdbc.queryForObject("SELECT id_alerta::text FROM alerta_vaciado_sanitario", String.class);

    var segunda = sincronizar.sincronizar();

    assertThat(segunda.exitosa()).isTrue();
    assertThat(cuenta("galpon")).isEqualTo(2);
    assertThat(cuenta("lote")).isEqualTo(2);
    assertThat(cuenta("alerta_vaciado_sanitario")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT id_alerta::text FROM alerta_vaciado_sanitario", String.class))
        .isEqualTo(alertaAntes);
    assertThat(cuenta("registro_sincronizacion")).isEqualTo(2); // una fila por ejecución
  }

  @Test
  void actualizaLaPoblacionCuandoM1LaCambia() {
    sincronizar.sincronizar();
    M1.responder(
        "/galpones/" + M1Simulado.GALPON_DOS,
        200,
        """
        {"id":"%s","nombre":"galpón demo 3","capacidad":4500,"estado":"AISLAMIENTO",
         "loteActivo":{"id":"%s","nombre":"lote demo 3","poblacionInicial":1000,"poblacionActual":950,
           "fechaIngreso":"2026-11-19","edadDias":3,"costoTotal":4000000}}
        """
            .formatted(M1Simulado.GALPON_DOS, M1Simulado.LOTE_ACTIVO));

    sincronizar.sincronizar();

    assertThat(
            jdbc.queryForObject(
                "SELECT poblacion_actual FROM lote WHERE id_lote = ?::uuid",
                Integer.class,
                M1Simulado.LOTE_ACTIVO))
        .isEqualTo(950);
    assertThat(
            jdbc.queryForObject(
                "SELECT poblacion_inicial FROM lote WHERE id_lote = ?::uuid",
                Integer.class,
                M1Simulado.LOTE_ACTIVO))
        .isEqualTo(1000); // la inicial no cambia
  }

  @Test
  void laBitacoraRegistraLaEjecucionExitosaYRt09LaVeActualizada() {
    sincronizar.sincronizar();

    assertThat(jdbc.queryForObject("SELECT resultado FROM registro_sincronizacion", String.class))
        .isEqualTo("EXITOSA");
    assertThat(jdbc.queryForObject("SELECT fuente FROM registro_sincronizacion", String.class))
        .isEqualTo("MODULO_1");
    assertThat(estado.consultar().modulo1().desactualizada()).isFalse();
    assertThat(estado.consultar().modulo1().ultimaSincronizacionExitosa()).isNotNull();
  }

  @Test
  void siM1CaeLaCopiaSeConservaLaEjecucionQuedaFallidaYRt09Advierte() {
    sincronizar.sincronizar();
    M1.limpiar(); // M1 pasa a responder 404 a la lista: lo mismo que perder las rutas
    M1.responder("/galpones?page=1", 500, "{\"status\":500,\"code\":\"ERROR_CONSULTA\"}");

    var resumen = sincronizar.sincronizar();

    assertThat(resumen.exitosa()).isFalse();
    assertThat(cuenta("galpon")).isEqualTo(2); // la copia anterior sigue intacta
    assertThat(cuenta("lote")).isEqualTo(2);
    assertThat(cuenta("alerta_vaciado_sanitario")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT resultado FROM registro_sincronizacion ORDER BY id DESC LIMIT 1",
                String.class))
        .isEqualTo("FALLIDA");
    assertThat(estado.consultar().modulo1().desactualizada()).isTrue();
    // Hay una sincronización exitosa anterior: RT-09 sigue mostrando su fecha
    assertThat(estado.consultar().modulo1().ultimaSincronizacionExitosa()).isNotNull();
  }

  @Test
  void unLoteInvalidoSeOmiteSinSobrescribirLoQueYaHabia() {
    sincronizar.sincronizar();
    M1.responder(
        "/galpones/" + M1Simulado.GALPON_DOS,
        200,
        """
        {"id":"%s","nombre":"galpón demo 3","capacidad":4500,"estado":"AISLAMIENTO",
         "loteActivo":{"id":"%s","nombre":"lote demo 3","poblacionInicial":1000,"poblacionActual":900,
           "fechaIngreso":"2026-11-19","costoTotal":0}}
        """
            .formatted(M1Simulado.GALPON_DOS, M1Simulado.LOTE_ACTIVO));

    var resumen = sincronizar.sincronizar();

    assertThat(resumen.exitosa()).isTrue();
    assertThat(resumen.incidencias()).hasSize(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT poblacion_actual FROM lote WHERE id_lote = ?::uuid",
                Integer.class,
                M1Simulado.LOTE_ACTIVO))
        .isEqualTo(1000); // el dato válido anterior no se pisa
    assertThat(
            jdbc.queryForObject(
                "SELECT descripcion_error FROM registro_sincronizacion ORDER BY id DESC LIMIT 1",
                String.class))
        .contains("costo");
  }

  private int cuenta(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
  }
}
