package co.edu.unimagdalena.avicontrol.infrastructure.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unimagdalena.avicontrol.TestcontainersConfiguration;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarEstadoSincronizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.GenerarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.PreviewLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.PublicarAvisosUtilizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo2UseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.GenerarLiquidacionCommand;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
 * CU08 a CU10 y el aviso de utilización de punta a punta: cliente REST real contra un M2 simulado y
 * PostgreSQL real. Verifica lo que importa al usuario: los datos de M2 llegan completos y con su
 * precisión, la Liquidación se calcula con ellos, el aviso sale una sola vez y la vista previa no
 * se invalida sola entre consultas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SincronizacionM2IT {

  private static final M2Simulado M2;

  static {
    try {
      M2 = new M2Simulado();
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registro) {
    registro.add("m2.base-url", M2::urlBase);
    // Las tareas programadas no deben correr solas durante la prueba: la prueba las invoca
    registro.add("sync.m2.retraso-inicial", () -> "PT1H");
    registro.add("avisos.retraso-inicial", () -> "PT1H");
  }

  @AfterAll
  static void detener() {
    M2.close();
  }

  private static final UUID LOTE = UUID.fromString(M2Simulado.LOTE);
  private static final UUID GALPON = UUID.fromString("a402a368-cd4d-4e27-8d48-9e0d7eb8d374");
  private static final UUID LOTE_SIN_ALERTA =
      UUID.fromString("2f545f14-128e-4e02-8b74-f00925789dc5");
  private static final UUID RESULTADO = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final String SUMINISTRO = "5a000000-0000-4000-8000-000000000001";
  private static final String RECEPCION_1 = "6b000000-0000-4000-8000-000000000001";
  private static final String RECEPCION_2 = "6b000000-0000-4000-8000-000000000002";
  private static final String CONSUMO = "7c000000-0000-4000-8000-000000000001";

  @Autowired private SincronizarModulo2UseCase sincronizar;
  @Autowired private PublicarAvisosUtilizacionUseCase publicar;
  @Autowired private PreviewLiquidacionUseCase vistaPrevia;
  @Autowired private GenerarLiquidacionUseCase generar;
  @Autowired private ConsultarEstadoSincronizacionUseCase estado;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void preparar() {
    jdbc.execute(
        "TRUNCATE alerta_vaciado_sanitario, lote, galpon, registro_sincronizacion RESTART IDENTITY"
            + " CASCADE");
    M2.limpiar();
    // Lo que ya trajo M1: un galpón, el lote por liquidar (con alerta) y otro lote aún productivo
    jdbc.update(
        "INSERT INTO galpon (id_galpon, nombre, aforo_maximo, estado, fecha_hora_sync)"
            + " VALUES (?, 'galpon demo', 5000, 'VACIADO_SANITARIO', now())",
        GALPON);
    insertarLote(LOTE, "lote demo");
    insertarLote(LOTE_SIN_ALERTA, "lote productivo");
    long registro =
        jdbc.queryForObject(
            "INSERT INTO registro_sincronizacion (fuente, topico, fecha_hora_inicio, resultado)"
                + " VALUES ('MODULO_1', 'GET /api/v1/galpones', now(), 'EXITOSA') RETURNING id",
            Long.class);
    jdbc.update(
        "INSERT INTO alerta_vaciado_sanitario (id_alerta, id_galpon, id_lote, fecha_hora_evento,"
            + " id_registro_sincronizacion) VALUES (?, ?, ?, now(), ?)",
        UUID.randomUUID(),
        GALPON,
        LOTE,
        registro);
    datosCompletosEnM2();
  }

  private void insertarLote(UUID id, String nombre) {
    jdbc.update(
        "INSERT INTO lote (id_lote, id_galpon, nombre, fecha_ingreso, poblacion_inicial,"
            + " poblacion_actual, costo_total_cop, estado, fecha_hora_sync)"
            + " VALUES (?, ?, ?, '2026-11-01', 2000, 1974, 5000000, 'DESVINCULADO', now())",
        id,
        GALPON,
        nombre);
  }

  /** Lo que entrega M2: resultado, dos partidas de alimento (dos recepciones) y un consumo. */
  private void datosCompletosEnM2() {
    M2.conResultado(M2Simulado.LOTE, RESULTADO.toString(), 1974, "4012.50");
    M2.conAlimentos(
        M2Simulado.LOTE,
        """
        [{"suministroId":"%s","recepcionId":"%s","nombreAlimento":"Inicio Premium",
          "tipoAlimento":"INICIO","cantidadKg":2500.5000,"precioNetoKg":2345.6789,
          "valorImpuestoCop":1114408.50,"datosCompletos":true},
         {"suministroId":"%s","recepcionId":"%s","nombreAlimento":"Inicio Premium",
          "tipoAlimento":"INICIO","cantidadKg":1000.2500,"precioNetoKg":2400.0000,
          "valorImpuestoCop":456114.00,"datosCompletos":true}]
        """
            .formatted(SUMINISTRO, RECEPCION_1, SUMINISTRO, RECEPCION_2));
    M2.conMedicamentos(
        M2Simulado.LOTE,
        """
        [{"consumoId":"%s","recepcionId":"%s","medicamentoId":"8d000000-0000-4000-8000-000000000001",
          "nombreMedicamento":"Enrofloxacina","fechaAplicacion":"2026-11-12",
          "cantidadUnidadBase":120000.0000,"unidadBase":"ML","precioNetoHistoricoUnidadBase":0.0333,
          "valorImpuestoCop":759.24,"datosCompletos":true},
         {"consumoId":"%s","recepcionId":"%s","medicamentoId":"8d000000-0000-4000-8000-000000000001",
          "nombreMedicamento":"Enrofloxacina","fechaAplicacion":"2026-11-12",
          "cantidadUnidadBase":20500.5000,"unidadBase":"ML","precioNetoHistoricoUnidadBase":0.0400,
          "valorImpuestoCop":155.80,"datosCompletos":true}]
        """
            .formatted(CONSUMO, RECEPCION_1, CONSUMO, RECEPCION_2));
  }

  // --- Copia local ---

  @Test
  void llenaLaCopiaLocalConLosDatosDeM2() {
    var resumen = sincronizar.sincronizar();

    assertThat(resumen.exitosa()).isTrue();
    assertThat(resumen.incidencias()).isEmpty();
    assertThat(cuenta("resultado_final_sacrificio")).isEqualTo(1);
    assertThat(cuenta("partida_alimento_lote")).isEqualTo(2); // una por recepción, sin consolidar
    assertThat(cuenta("consumo_medicamento_lote")).isEqualTo(1);
    assertThat(cuenta("tramo_recepcion_consumo")).isEqualTo(2);
    assertThat(resumen.registrosActualizados()).isEqualTo(1 + 2 + 1 + 2);
    // El galpón de la fila es el del lote local, no el que diga M2
    assertThat(
            jdbc.queryForObject(
                "SELECT id_galpon::text FROM resultado_final_sacrificio", String.class))
        .isEqualTo(GALPON.toString());
    assertThat(
            jdbc.queryForObject(
                "SELECT peso_total_kg FROM resultado_final_sacrificio", BigDecimal.class))
        .isEqualByComparingTo("4012.50");
    // El consumo suma sus tramos
    assertThat(
            jdbc.queryForObject(
                "SELECT cantidad_unidad_base FROM consumo_medicamento_lote", BigDecimal.class))
        .isEqualByComparingTo("140500.5");
    assertThat(
            jdbc.queryForObject("SELECT medicamento FROM consumo_medicamento_lote", String.class))
        .isEqualTo("Enrofloxacina");
  }

  @Test
  void guardaLosPreciosYLasCantidadesConLos4DecimalesDeM2() {
    sincronizar.sincronizar();

    assertThat(
            jdbc.queryForObject(
                "SELECT precio_unitario_kg_cop FROM partida_alimento_lote"
                    + " WHERE cantidad_kg = 2500.5",
                BigDecimal.class))
        .isEqualByComparingTo("2345.6789"); // con 2 decimales sería 2345.68
    assertThat(
            jdbc.queryForObject(
                "SELECT precio_unitario_base_cop FROM tramo_recepcion_consumo"
                    + " WHERE referencia_recepcion = ?",
                BigDecimal.class,
                RECEPCION_1))
        .isEqualByComparingTo("0.0333"); // con 2 decimales sería 0.03: 10 % de error
    assertThat(
            jdbc.queryForObject(
                "SELECT valor_impuesto_cop FROM partida_alimento_lote WHERE cantidad_kg = 2500.5",
                BigDecimal.class))
        .isEqualByComparingTo("1114408.50");
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_valorizacion FROM partida_alimento_lote LIMIT 1", String.class))
        .isEqualTo("VALORIZADA");
  }

  @Test
  void repetirNoDuplicaNiRenuevaLaFechaDeSincronizacionSiNadaCambio() {
    sincronizar.sincronizar();
    var antes =
        jdbc.queryForObject(
            "SELECT (SELECT fecha_hora_sync FROM resultado_final_sacrificio)::text || '|'"
                + " || (SELECT max(fecha_hora_sync) FROM partida_alimento_lote)::text || '|'"
                + " || (SELECT fecha_hora_sync FROM consumo_medicamento_lote)::text",
            String.class);

    var segunda = sincronizar.sincronizar();

    assertThat(segunda.exitosa()).isTrue();
    assertThat(cuenta("partida_alimento_lote")).isEqualTo(2);
    assertThat(cuenta("tramo_recepcion_consumo")).isEqualTo(2);
    assertThat(cuenta("registro_sincronizacion WHERE fuente = 'MODULO_2'")).isEqualTo(2);
    var despues =
        jdbc.queryForObject(
            "SELECT (SELECT fecha_hora_sync FROM resultado_final_sacrificio)::text || '|'"
                + " || (SELECT max(fecha_hora_sync) FROM partida_alimento_lote)::text || '|'"
                + " || (SELECT fecha_hora_sync FROM consumo_medicamento_lote)::text",
            String.class);
    assertThat(despues).isEqualTo(antes);
  }

  @Test
  void laVistaPreviaNoSeInvalidaPorUnaConsultaPosteriorSinCambios() {
    sincronizar.sincronizar();
    String huella = vistaPrevia.calcularVistaPrevia(comando(null)).huellaDatos();

    sincronizar.sincronizar(); // M2 y M3 no cambiaron

    String despues = vistaPrevia.calcularVistaPrevia(comando(null)).huellaDatos();
    assertThat(despues).isEqualTo(huella);
  }

  @Test
  void siM2CambiaUnDatoSeActualizaYLaVistaPreviaAnteriorSeInvalida() {
    sincronizar.sincronizar();
    String huella = vistaPrevia.calcularVistaPrevia(comando(null)).huellaDatos();
    M2.conResultado(M2Simulado.LOTE, RESULTADO.toString(), 1950, "3990.00"); // M2 corrigió

    sincronizar.sincronizar();

    assertThat(
            jdbc.queryForObject(
                "SELECT cantidad_final_pollos FROM resultado_final_sacrificio", Integer.class))
        .isEqualTo(1950);
    assertThat(vistaPrevia.calcularVistaPrevia(comando(null)).huellaDatos()).isNotEqualTo(huella);
  }

  @Test
  void unResultadoYaUtilizadoNoSePisaConUnaCorreccionPosterior() {
    sincronizar.sincronizar();
    jdbc.update("UPDATE resultado_final_sacrificio SET estado_utilizacion = 'UTILIZADO'");
    M2.conResultado(M2Simulado.LOTE, RESULTADO.toString(), 1500, "3000.00");

    sincronizar.sincronizar();

    assertThat(
            jdbc.queryForObject(
                "SELECT cantidad_final_pollos FROM resultado_final_sacrificio", Integer.class))
        .isEqualTo(1974);
  }

  @Test
  void soloConsultaLosLotesConAlertaDeVaciado() {
    sincronizar.sincronizar();

    assertThat(M2.peticiones()).isNotEmpty();
    assertThat(M2.peticiones())
        .allSatisfy(
            p ->
                assertThat(p.ruta())
                    .contains(M2Simulado.LOTE)
                    .doesNotContain(LOTE_SIN_ALERTA.toString()));
  }

  @Test
  void unLoteSinResultadoNiPartidasNoEscribeNadaYLaEjecucionEsExitosa() {
    M2.limpiar();
    M2.sinResultado(M2Simulado.LOTE);
    M2.conAlimentos(M2Simulado.LOTE, "[]");
    M2.conMedicamentos(M2Simulado.LOTE, "[]");

    var resumen = sincronizar.sincronizar();

    assertThat(resumen.exitosa()).isTrue();
    assertThat(resumen.registrosActualizados()).isZero();
    assertThat(cuenta("resultado_final_sacrificio")).isZero();
    assertThat(estado.consultar().modulo2().desactualizada()).isFalse();
  }

  // --- Bitácora y RT-09 ---

  @Test
  void laBitacoraRegistraLaEjecucionYRt09VeM2Actualizado() {
    sincronizar.sincronizar();

    assertThat(
            jdbc.queryForObject(
                "SELECT resultado FROM registro_sincronizacion WHERE fuente = 'MODULO_2'",
                String.class))
        .isEqualTo("EXITOSA");
    assertThat(estado.consultar().modulo2().desactualizada()).isFalse();
    assertThat(estado.consultar().modulo2().ultimaSincronizacionExitosa()).isNotNull();
  }

  @Test
  void siM2CaeLaCopiaSeConservaLaEjecucionQuedaFallidaYRt09Advierte() {
    sincronizar.sincronizar();
    M2.limpiar();
    M2.responder("GET", "/lotes/" + M2Simulado.LOTE + "/resultado-sacrificio", 500, "{}");

    var resumen = sincronizar.sincronizar();

    assertThat(resumen.exitosa()).isFalse();
    assertThat(cuenta("partida_alimento_lote")).isEqualTo(2); // la copia anterior sigue intacta
    assertThat(cuenta("resultado_final_sacrificio")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT resultado FROM registro_sincronizacion WHERE fuente = 'MODULO_2'"
                    + " ORDER BY id DESC LIMIT 1",
                String.class))
        .isEqualTo("FALLIDA");
    assertThat(estado.consultar().modulo2().desactualizada()).isTrue();
  }

  @Test
  void unConsumoInvalidoSeOmiteSinPisarLoQueYaHabia() {
    sincronizar.sincronizar();
    M2.conMedicamentos(
        M2Simulado.LOTE,
        """
        [{"consumoId":"%s","recepcionId":"%s","nombreMedicamento":"Enrofloxacina",
          "fechaAplicacion":"2026-11-12","cantidadUnidadBase":0,"unidadBase":"ML",
          "precioNetoHistoricoUnidadBase":0.0333}]
        """
            .formatted(CONSUMO, RECEPCION_1));

    var resumen = sincronizar.sincronizar();

    assertThat(resumen.exitosa()).isTrue();
    assertThat(resumen.incidencias()).hasSize(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT cantidad_unidad_base FROM consumo_medicamento_lote", BigDecimal.class))
        .isEqualByComparingTo("140500.5");
  }

  // --- Liquidación con los datos de M2 ---

  @Test
  void laLiquidacionSeCalculaConLosPreciosCompletosDeM2() {
    sincronizar.sincronizar();
    var previa = vistaPrevia.calcularVistaPrevia(comando(null));

    var liquidacion = generar.generarLiquidacion(comando(previa.huellaDatos()));

    // 2500.5 x 2345.6789 = 5 865 370.09 y 1000.25 x 2400 = 2 400 600 (COP enteros, RT-01)
    assertThat(subtotal("ALIMENTO", "2500.5000")).isEqualTo(5_865_370L);
    assertThat(subtotal("ALIMENTO", "1000.2500")).isEqualTo(2_400_600L);
    // 120000 x 0.0333 = 3 996 y 20500.5 x 0.04 = 820.02: con 2 decimales darían 3 600 y 820
    assertThat(subtotal("MEDICINA", "120000.0000")).isEqualTo(3_996L);
    assertThat(subtotal("MEDICINA", "20500.5000")).isEqualTo(820L);
    assertThat(
            jdbc.queryForObject(
                "SELECT precio_unitario_cop FROM partida_costo_lote WHERE cantidad = 2500.5",
                BigDecimal.class))
        .isEqualByComparingTo("2345.6789"); // el desglose muestra el precio sin redondear
    assertThat(liquidacion.costosOperativosCop())
        .isEqualTo(5_000_000L + 5_865_370L + 2_400_600L + 3_996L + 820L);
  }

  @Test
  void despuesDeLiquidarElAvisoSaleUnaSolaVezConElMismoEventoYM2YaNoSeConsultaPorEseLote() {
    sincronizar.sincronizar();
    var previa = vistaPrevia.calcularVistaPrevia(comando(null));
    generar.generarLiquidacion(comando(previa.huellaDatos()));
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_utilizacion FROM resultado_final_sacrificio", String.class))
        .isEqualTo("UTILIZADO");
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_entrega FROM aviso_utilizacion_resultado", String.class))
        .isEqualTo("PENDIENTE");
    M2.limpiar();
    M2.responder(
        "POST",
        "/resultados-sacrificio/utilizado",
        200,
        """
        {"eventId":"%s","resultadoId":"%s","loteId":"%s","estado":"UTILIZADO"}
        """
            .formatted(UUID.randomUUID(), RESULTADO, M2Simulado.LOTE));

    var primera = publicar.publicar();
    var segunda = publicar.publicar();

    assertThat(primera.entregados()).isEqualTo(1);
    assertThat(segunda.entregados()).isZero(); // ya está entregado: no se vuelve a enviar
    assertThat(M2.peticiones()).hasSize(1);
    var envio = M2.peticiones().get(0);
    UUID eventoEsperado =
        UUID.nameUUIDFromBytes(
            ("aviso-resultado-utilizado:" + RESULTADO).getBytes(StandardCharsets.UTF_8));
    assertThat(envio.cuerpo()).contains("\"eventId\":\"" + eventoEsperado + "\"");
    assertThat(envio.cuerpo()).contains("\"resultadoId\":\"" + RESULTADO + "\"");
    assertThat(envio.conAutorizacion()).isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_entrega FROM aviso_utilizacion_resultado", String.class))
        .isEqualTo("ENTREGADO");

    // El lote ya tiene Liquidación ACTIVA: M3 deja de consultar a M2 por él
    M2.limpiar();
    sincronizar.sincronizar();
    assertThat(M2.peticiones()).isEmpty();
  }

  @Test
  void siM2NoConfirmaElAvisoQuedaPendienteYSeReintentaConElMismoEvento() {
    sincronizar.sincronizar();
    var previa = vistaPrevia.calcularVistaPrevia(comando(null));
    generar.generarLiquidacion(comando(previa.huellaDatos()));
    M2.limpiar();
    M2.responder("POST", "/resultados-sacrificio/utilizado", 500, "{\"status\":500}");

    var fallida = publicar.publicar();

    assertThat(fallida.entregados()).isZero();
    assertThat(fallida.fallidos()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_entrega FROM aviso_utilizacion_resultado", String.class))
        .isEqualTo("PENDIENTE");
    assertThat(
            jdbc.queryForObject("SELECT intentos FROM aviso_utilizacion_resultado", Integer.class))
        .isEqualTo(1);

    M2.responder(
        "POST",
        "/resultados-sacrificio/utilizado",
        200,
        """
        {"eventId":"%s","resultadoId":"%s","loteId":"%s","estado":"UTILIZADO"}
        """
            .formatted(UUID.randomUUID(), RESULTADO, M2Simulado.LOTE));
    var exitosa = publicar.publicar();

    assertThat(exitosa.entregados()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_entrega FROM aviso_utilizacion_resultado", String.class))
        .isEqualTo("ENTREGADO");
    assertThat(
            jdbc.queryForObject("SELECT intentos FROM aviso_utilizacion_resultado", Integer.class))
        .isEqualTo(2);
    // Los dos envíos llevan exactamente el mismo cuerpo, como exige M2 para que el reintento sea
    // seguro
    assertThat(M2.peticiones()).hasSize(2);
    assertThat(M2.peticiones().get(1).cuerpo()).isEqualTo(M2.peticiones().get(0).cuerpo());
  }

  private GenerarLiquidacionCommand comando(String huella) {
    return new GenerarLiquidacionCommand(LOTE, new BigDecimal("7000"), "Juan Grau", huella);
  }

  private long subtotal(String categoria, String cantidad) {
    return jdbc.queryForObject(
        "SELECT subtotal_cop FROM partida_costo_lote WHERE categoria = ? AND cantidad = ?::numeric",
        Long.class,
        categoria,
        cantidad);
  }

  private int cuenta(String tablaYFiltro) {
    return jdbc.queryForObject("SELECT count(*) FROM " + tablaYFiltro, Integer.class);
  }
}
