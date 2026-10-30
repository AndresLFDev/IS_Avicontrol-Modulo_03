package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unimagdalena.avicontrol.TestcontainersConfiguration;
import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaExistenteException;
import co.edu.unimagdalena.avicontrol.domain.port.in.GenerarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.PreviewLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.GenerarLiquidacionCommand;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Plan 003, T010: M3-CU03 de punta a punta contra PostgreSQL real, con los datos del Caso Dorado
 * (CU03 esc. 1), el siniestro total y la partida sin precio del seed de demostración.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class LiquidacionIT {

  private static final UUID DORADO = UUID.fromString("b0000000-0000-0000-0000-000000000003");
  private static final UUID SINIESTRO = UUID.fromString("b0000000-0000-0000-0000-000000000004");
  private static final UUID SIN_PRECIO = UUID.fromString("b0000000-0000-0000-0000-000000000006");
  private static final UUID RESULTADO = UUID.fromString("d0000000-0000-0000-0000-000000000003");

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PreviewLiquidacionUseCase vistaPrevia;
  @Autowired private GenerarLiquidacionUseCase generar;

  @BeforeEach
  void cargarCopiaLocal() {
    jdbc.execute(
        "TRUNCATE registro_anulacion, snapshot_datos_origen, partida_costo_lote, liquidacion,"
            + " aviso_utilizacion_resultado, resultado_final_sacrificio, alerta_vaciado_sanitario,"
            + " tramo_recepcion_consumo, consumo_medicamento_lote, partida_alimento_lote,"
            + " lote, galpon, registro_sincronizacion RESTART IDENTITY CASCADE");
    long registro =
        jdbc.queryForObject(
            "INSERT INTO registro_sincronizacion (fuente, topico, fecha_hora_inicio, resultado)"
                + " VALUES ('MODULO_1', 'avicontrol.m1.alertas-vaciado', now(), 'EXITOSA')"
                + " RETURNING id",
            Long.class);
    lote(DORADO, "a0000000-0000-0000-0000-000000000003", "L-2026-03", 9000, 8500, 18_000_000);
    lote(SINIESTRO, "a0000000-0000-0000-0000-000000000004", "L-2026-04", 8000, 0, 10_000_000);
    lote(SIN_PRECIO, "a0000000-0000-0000-0000-000000000006", "L-2026-06", 8800, 8650, 17_600_000);
    for (var lote : new UUID[] {DORADO, SINIESTRO, SIN_PRECIO}) {
      jdbc.update(
          "INSERT INTO alerta_vaciado_sanitario (id_alerta, id_galpon, id_lote, fecha_hora_evento,"
              + " id_registro_sincronizacion)"
              + " SELECT gen_random_uuid(), id_galpon, id_lote, now(), ? FROM lote WHERE id_lote = ?",
          registro,
          lote);
    }
    jdbc.update(
        "INSERT INTO resultado_final_sacrificio (id_resultado, id_lote, id_galpon,"
            + " cantidad_final_pollos, peso_total_kg, fecha_registro, fecha_hora_sync)"
            + " SELECT ?, id_lote, id_galpon, 8500, 23800.00, now(), TIMESTAMPTZ"
            + " '2026-10-20 09:00:01-05' FROM lote WHERE id_lote = ?",
        RESULTADO,
        DORADO);
    jdbc.update(
        "INSERT INTO resultado_final_sacrificio (id_resultado, id_lote, id_galpon,"
            + " cantidad_final_pollos, peso_total_kg, fecha_registro, fecha_hora_sync)"
            + " SELECT gen_random_uuid(), id_lote, id_galpon, 8600, 24510.00, now(), now()"
            + " FROM lote WHERE id_lote = ?",
        SIN_PRECIO);
    alimento(DORADO, "Pre-inicio", "6000", "2600");
    alimento(DORADO, "Inicio", "12000", "2500");
    alimento(DORADO, "Engorde", "8000", "2400");
    alimento(SINIESTRO, "Inicio", "7000", "3000");
    alimento(SIN_PRECIO, "Inicio", "11000", "2500");
    alimento(SIN_PRECIO, "Engorde", "9000", null);
    medicamento(
        DORADO,
        "Amoxicilina",
        "50",
        new String[][] {{"REC-090", "30", "20000"}, {"REC-101", "20", "21000"}});
    medicamento(DORADO, "Vacuna Newcastle", "9440", new String[][] {{"REC-085", "9440", "125"}});
    medicamento(SINIESTRO, "Enrofloxacina", "90", new String[][] {{"REC-095", "90", "20000"}});
  }

  @Test
  void generaLaLiquidacionDelCasoDoradoYCongelaSusDatos() throws Exception {
    String huella =
        previa(DORADO, "4500")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ventaBrutaCop").value(107_100_000))
            .andExpect(jsonPath("$.mortalidadAves").value(500))
            .andExpect(jsonPath("$.porcentajeMortalidad").value(5.56))
            .andExpect(jsonPath("$.subtotalesCategoria.alimento").value(64_800_000))
            .andExpect(jsonPath("$.subtotalesCategoria.insumosMedicos").value(2_200_000))
            .andExpect(jsonPath("$.subtotalesCategoria.costoPoblacion").value(18_000_000))
            .andExpect(jsonPath("$.costosOperativosCop").value(85_000_000))
            .andExpect(jsonPath("$.utilidadNetaCop").value(22_100_000))
            .andExpect(jsonPath("$.pesoPromedioKg").value(2.80))
            .andExpect(jsonPath("$.fechaSincronizacionM2").value("2026-10-20T14:20:01Z"))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .transform(json -> JsonPath.read(json, "$.huellaDatos"));
    assertThat(cuenta("liquidacion")).isZero();

    confirmar(DORADO, "4500", huella)
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/liquidaciones/1"))
        .andExpect(jsonPath("$.estado").value("ACTIVA"))
        .andExpect(jsonPath("$.utilidadNetaCop").value(22_100_000));

    mvc.perform(get("/api/v1/liquidaciones/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreGalpon").value("Galpón L-2026-03"))
        .andExpect(jsonPath("$.precioKgCop").value(4500.00))
        .andExpect(jsonPath("$.pesoPromedioKg").value(2.80))
        .andExpect(jsonPath("$.subtotalesCategoria.insumosMedicos").value(2_200_000))
        .andExpect(jsonPath("$.usuarioResponsable").value("Jorge Meléndez"))
        .andExpect(jsonPath("$.fechaSincronizacionM1").value("2026-10-20T13:00:01Z"))
        .andExpect(jsonPath("$.accionesDisponibles.length()").value(2));

    assertThat(cuenta("partida_costo_lote")).isEqualTo(7);
    assertThat(cuenta("snapshot_datos_origen")).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_utilizacion FROM resultado_final_sacrificio WHERE id_resultado = ?",
                String.class,
                RESULTADO))
        .isEqualTo("UTILIZADO");
    assertThat(
            jdbc.queryForObject(
                "SELECT estado_entrega FROM aviso_utilizacion_resultado", String.class))
        .isEqualTo("PENDIENTE");
    // FR-014 de CU01: el lote liquidado sale de la lista
    mvc.perform(get("/api/v1/lotes").param("search", "L-2026-03"))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void unaSegundaLiquidacionDelLoteResponde409() throws Exception {
    confirmar(DORADO, "4500", huella(DORADO, "4500")).andExpect(status().isCreated());

    previa(DORADO, "4500")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.idLiquidacionActiva").value(1));
  }

  @Test
  void siLosDatosCambianTrasLaVistaPreviaNoPersiste() throws Exception {
    var huella = huella(DORADO, "4500");
    jdbc.update("UPDATE lote SET poblacion_actual = 8499 WHERE id_lote = ?", DORADO);

    confirmar(DORADO, "4500", huella)
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.type")
                .value("https://avicontrol.edu.co/errors/vista-previa-desactualizada"));
    assertThat(cuenta("liquidacion")).isZero();
  }

  @Test
  void siniestroTotalSeLiquidaSinPrecioNiAviso() throws Exception {
    var huella = huella(SINIESTRO, null);

    confirmar(SINIESTRO, null, huella)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.siniestroTotal").value(true))
        .andExpect(jsonPath("$.ventaBrutaCop").value(0))
        .andExpect(jsonPath("$.precioKgCop").doesNotExist())
        .andExpect(jsonPath("$.porcentajeMortalidad").value(100.00))
        .andExpect(jsonPath("$.costosOperativosCop").value(32_800_000))
        .andExpect(jsonPath("$.utilidadNetaCop").value(-32_800_000));
    assertThat(cuenta("aviso_utilizacion_resultado")).isZero();
  }

  @Test
  void unaPartidaSinPrecioBloqueaConSuDetalle() throws Exception {
    previa(SIN_PRECIO, "4500")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.causa").value("PARTIDAS_SIN_PRECIO"))
        .andExpect(jsonPath("$.partidasPendientes[0].concepto").value("Engorde"));
  }

  @Test
  void trasAnularSePuedeReliquidarYElAvisoNoSeDuplica() throws Exception {
    confirmar(DORADO, "4500", huella(DORADO, "4500")).andExpect(status().isCreated());
    // CU04 aún no existe: se anula directamente en la base
    jdbc.update("UPDATE liquidacion SET estado = 'ANULADA' WHERE id_liquidacion = 1");
    jdbc.update(
        "INSERT INTO registro_anulacion (id_liquidacion, motivo, fecha_hora_anulacion,"
            + " usuario_responsable) VALUES (1, 'Error en el precio por kg', now(), 'Ana')");

    confirmar(DORADO, "4600", huella(DORADO, "4600"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.idLiquidacion").value(2))
        .andExpect(jsonPath("$.ventaBrutaCop").value(109_480_000));

    mvc.perform(get("/api/v1/liquidaciones/1"))
        .andExpect(jsonPath("$.estado").value("ANULADA"))
        .andExpect(jsonPath("$.registroAnulacion.motivo").value("Error en el precio por kg"))
        .andExpect(jsonPath("$.accionesDisponibles.length()").value(1));
    assertThat(cuenta("aviso_utilizacion_resultado")).isEqualTo(1);
  }

  @Test
  void dosConfirmacionesSimultaneasDejanUnaSolaActiva() throws Exception {
    var precio = new BigDecimal("4500");
    var huella =
        vistaPrevia
            .calcularVistaPrevia(new GenerarLiquidacionCommand(DORADO, precio, "Ana", null))
            .huellaDatos();
    var comando = new GenerarLiquidacionCommand(DORADO, precio, "Ana", huella);
    var salida = new CountDownLatch(1);
    Callable<Object> confirmacion =
        () -> {
          salida.await();
          return generar.generarLiquidacion(comando);
        };

    try (var hilos = Executors.newFixedThreadPool(2)) {
      var futuros = new ArrayList<java.util.concurrent.Future<Object>>();
      futuros.add(hilos.submit(confirmacion));
      futuros.add(hilos.submit(confirmacion));
      salida.countDown();
      int exitos = 0;
      int conflictos = 0;
      for (var futuro : futuros) {
        try {
          futuro.get();
          exitos++;
        } catch (ExecutionException e) {
          assertThat(e.getCause()).isInstanceOf(LiquidacionYaExistenteException.class);
          conflictos++;
        }
      }
      assertThat(exitos).isEqualTo(1);
      assertThat(conflictos).isEqualTo(1);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM liquidacion WHERE estado = 'ACTIVA'", Integer.class))
        .isEqualTo(1);
  }

  private ResultActions previa(UUID lote, String precio) throws Exception {
    return mvc.perform(
        post("/api/v1/liquidaciones/previa")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"idLote":"%s","precioKgCop":%s,"usuario":"Jorge Meléndez"}
                """
                    .formatted(lote, precio)));
  }

  private String huella(UUID lote, String precio) throws Exception {
    return JsonPath.read(
        previa(lote, precio).andReturn().getResponse().getContentAsString(), "$.huellaDatos");
  }

  private ResultActions confirmar(UUID lote, String precio, String huella) throws Exception {
    return mvc.perform(
        post("/api/v1/liquidaciones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"idLote":"%s","precioKgCop":%s,"usuario":"Jorge Meléndez","huellaDatos":"%s"}
                """
                    .formatted(lote, precio, huella)));
  }

  private int cuenta(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
  }

  private void lote(UUID id, String galpon, String nombre, int inicial, int actual, long costo) {
    jdbc.update(
        "INSERT INTO galpon (id_galpon, nombre, aforo_maximo, estado, fecha_hora_sync)"
            + " VALUES (?, ?, 10000, 'VACIADO_SANITARIO', now())",
        UUID.fromString(galpon),
        "Galpón " + nombre);
    jdbc.update(
        "INSERT INTO lote (id_lote, id_galpon, nombre, fecha_ingreso, poblacion_inicial,"
            + " poblacion_actual, costo_total_cop, estado, fecha_hora_sync)"
            + " VALUES (?, ?, ?, DATE '2026-08-01', ?, ?, ?, 'CERRADO',"
            + " TIMESTAMPTZ '2026-10-20 08:00:01-05')",
        id,
        UUID.fromString(galpon),
        nombre,
        inicial,
        actual,
        costo);
  }

  private void alimento(UUID lote, String tipo, String cantidad, String precio) {
    jdbc.update(
        "INSERT INTO partida_alimento_lote (id_partida_origen, id_lote, id_galpon, tipo_alimento,"
            + " cantidad_kg, precio_unitario_kg_cop, estado_valorizacion, referencia_origen,"
            + " fecha_hora_sync)"
            + " SELECT gen_random_uuid(), id_lote, id_galpon, ?, ?, ?, ?, 'REC-1',"
            + " TIMESTAMPTZ '2026-10-20 09:10:01-05' FROM lote WHERE id_lote = ?",
        tipo,
        new BigDecimal(cantidad),
        precio == null ? null : new BigDecimal(precio),
        precio == null ? "PENDIENTE_PRECIO" : "VALORIZADA",
        lote);
  }

  private void medicamento(UUID lote, String nombre, String cantidad, String[][] tramos) {
    UUID consumo = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO consumo_medicamento_lote (id_consumo_origen, id_lote, id_galpon, medicamento,"
            + " fecha_consumo, cantidad_unidad_base, unidad_base, fecha_hora_sync)"
            + " SELECT ?, id_lote, id_galpon, ?, DATE '2026-08-20', ?, 'ML',"
            + " TIMESTAMPTZ '2026-10-20 09:20:01-05' FROM lote WHERE id_lote = ?",
        consumo,
        nombre,
        new BigDecimal(cantidad),
        lote);
    for (var tramo : tramos) {
      jdbc.update(
          "INSERT INTO tramo_recepcion_consumo (id_consumo_origen, referencia_recepcion,"
              + " cantidad_unidad_base, precio_unitario_base_cop) VALUES (?, ?, ?, ?)",
          consumo,
          tramo[0],
          new BigDecimal(tramo[1]),
          new BigDecimal(tramo[2]));
    }
  }
}
