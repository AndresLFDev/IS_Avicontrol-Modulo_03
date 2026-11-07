package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import co.edu.unimagdalena.avicontrol.TestcontainersConfiguration;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarHistorialUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialItemDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Plan 004, T004: búsqueda, rango de fechas en la zona de Colombia, orden y paginación de M3-CU06
 * contra PostgreSQL real.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class HistorialLiquidacionesIT {

  @Autowired private ConsultarHistorialUseCase casoDeUso;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE registro_anulacion, snapshot_datos_origen, partida_costo_lote, liquidacion,"
            + " aviso_utilizacion_resultado, resultado_final_sacrificio, alerta_vaciado_sanitario,"
            + " lote, galpon, registro_sincronizacion RESTART IDENTITY CASCADE");
  }

  @Test
  void ordenaDeLaMasRecienteALaMasAntiguaEIncluyeLasAnuladasDelMismoLote() {
    UUID galpon = galpon("Galpón Norte");
    UUID lote = lote(galpon, "L-01");
    long anulada = liquidacion(lote, galpon, "ANULADA", "2026-09-20T09:15:00Z");
    long activa = liquidacion(lote, galpon, "ACTIVA", "2026-09-28T16:42:00Z");

    var pagina = casoDeUso.consultarHistorial("l-01", null, null, 0, 10);

    assertThat(pagina.content())
        .extracting(HistorialItemDto::idLiquidacion, HistorialItemDto::estado)
        .containsExactly(tuple(activa, "ACTIVA"), tuple(anulada, "ANULADA"));
    var fila = pagina.content().getFirst();
    assertThat(fila.nombreGalpon()).isEqualTo("Galpón Norte");
    assertThat(fila.nombreLote()).isEqualTo("L-01");
    assertThat(fila.idLote()).isEqualTo(lote);
    assertThat(fila.idGalpon()).isEqualTo(galpon);
    assertThat(fila.fechaHoraGeneracion()).isEqualTo(Instant.parse("2026-09-28T16:42:00Z"));
    assertThat(fila.ventaBrutaCop()).isEqualTo(100);
    assertThat(fila.porcentajeMortalidad()).isEqualByComparingTo("4.00");
    assertThat(fila.costosOperativosCop()).isEqualTo(60);
    assertThat(fila.utilidadNetaCop()).isEqualTo(40);
    assertThat(pagina.mensaje()).isNull();
  }

  @Test
  void buscaPorNombreYPorUuidDeGalponYDeLote() {
    UUID norte = galpon("Galpón Norte");
    UUID sur = galpon("Galpón Sur");
    UUID loteNorte = lote(norte, "L-01");
    long enNorte = liquidacion(loteNorte, norte, "ACTIVA", "2026-09-01T15:00:00Z");
    long enSur = liquidacion(lote(sur, "L-02"), sur, "ACTIVA", "2026-09-02T15:00:00Z");

    assertThat(ids("galpón nor")).containsExactly(enNorte);
    assertThat(ids("L-02")).containsExactly(enSur);
    assertThat(ids(loteNorte.toString().toUpperCase())).containsExactly(enNorte);
    assertThat(ids(sur.toString())).containsExactly(enSur);
    assertThat(ids(null)).containsExactly(enSur, enNorte);
  }

  @Test
  void losComodinesDeLaBusquedaSonLiterales() {
    UUID galpon = galpon("Galpón Norte");
    liquidacion(lote(galpon, "L-01"), galpon, "ACTIVA", "2026-09-01T15:00:00Z");

    assertThat(ids("%")).isEmpty();
    assertThat(ids("L_01")).isEmpty();
  }

  @Test
  void elRangoUsaElDiaDeColombiaEIncluyeSusExtremos() {
    UUID galpon = galpon("Galpón Norte");
    UUID lote = lote(galpon, "L-01");
    // El 1 de marzo en Colombia va de 05:00 UTC del 1 a 05:00 UTC del 2
    liquidacion(lote, galpon, "ANULADA", "2026-03-01T04:59:59Z"); // 28 de febrero en Colombia
    long primera = liquidacion(lote, galpon, "ANULADA", "2026-03-01T05:00:00Z");
    long ultima = liquidacion(lote, galpon, "ANULADA", "2026-03-02T04:59:59.999Z");
    liquidacion(lote, galpon, "ACTIVA", "2026-03-02T05:00:00Z"); // 2 de marzo en Colombia

    var dia = LocalDate.of(2026, 3, 1);
    var pagina = casoDeUso.consultarHistorial(null, dia, dia, 0, 10);

    assertThat(pagina.content())
        .extracting(HistorialItemDto::idLiquidacion)
        .containsExactly(ultima, primera);
    assertThat(pagina.totalElements()).isEqualTo(2);
  }

  @Test
  void conservaElGalponHistoricoSiElLoteCambioDeGalpon() {
    UUID original = galpon("Galpón Norte");
    UUID nuevo = galpon("Galpón Sur");
    UUID lote = lote(original, "L-01");
    long liquidacion = liquidacion(lote, original, "ACTIVA", "2026-09-01T15:00:00Z");
    jdbc.update("UPDATE lote SET id_galpon = ? WHERE id_lote = ?", nuevo, lote);

    assertThat(ids("Galpón Norte")).containsExactly(liquidacion);
    assertThat(ids("Galpón Sur")).isEmpty();
  }

  @Test
  void paginaConElTotalRealDelFiltro() {
    UUID galpon = galpon("Galpón Norte");
    UUID lote = lote(galpon, "L-01");
    for (int dia = 1; dia <= 12; dia++) {
      liquidacion(lote, galpon, "ANULADA", "2026-09-%02dT15:00:00Z".formatted(dia));
    }

    var primera = casoDeUso.consultarHistorial(null, null, null, 0, 10);
    var segunda = casoDeUso.consultarHistorial(null, null, null, 1, 10);

    assertThat(primera.content()).hasSize(10);
    assertThat(segunda.content()).hasSize(2);
    assertThat(segunda.totalElements()).isEqualTo(12);
    assertThat(segunda.totalPages()).isEqualTo(2);
    assertThat(segunda.content().getLast().fechaHoraGeneracion())
        .isEqualTo(Instant.parse("2026-09-01T15:00:00Z"));
  }

  @Test
  void distingueLosDosMensajesDeHistorialVacio() {
    assertThat(casoDeUso.consultarHistorial(null, null, null, 0, 10).mensaje())
        .isEqualTo("No hay liquidaciones registradas aún");

    UUID galpon = galpon("Galpón Norte");
    liquidacion(lote(galpon, "L-01"), galpon, "ACTIVA", "2026-09-01T15:00:00Z");

    assertThat(casoDeUso.consultarHistorial("no-existe", null, null, 0, 10).mensaje())
        .isEqualTo("No se encontraron liquidaciones para los criterios de búsqueda aplicados");
  }

  private List<Long> ids(String search) {
    return casoDeUso.consultarHistorial(search, null, null, 0, 10).content().stream()
        .map(HistorialItemDto::idLiquidacion)
        .toList();
  }

  private UUID galpon(String nombre) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO galpon (id_galpon, nombre, aforo_maximo, estado, fecha_hora_sync)"
            + " VALUES (?, ?, 10000, 'DISPONIBLE', now())",
        id,
        nombre);
    return id;
  }

  private UUID lote(UUID galpon, String nombre) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO lote (id_lote, id_galpon, nombre, fecha_ingreso, poblacion_inicial,"
            + " poblacion_actual, costo_total_cop, estado, fecha_hora_sync)"
            + " VALUES (?, ?, ?, DATE '2026-08-01', 10000, 9600, 20000000, 'CERRADO', now())",
        id,
        galpon,
        nombre);
    return id;
  }

  private long liquidacion(UUID lote, UUID galpon, String estado, String generada) {
    return jdbc.queryForObject(
        "INSERT INTO liquidacion (id_lote, id_galpon, pollos_vendidos, peso_total_kg,"
            + " venta_bruta_cop, mortalidad_aves, porcentaje_mortalidad, costos_operativos_cop,"
            + " utilidad_neta_cop, estado, fecha_hora_generacion, usuario_responsable)"
            + " VALUES (?, ?, 0, 0, 100, 400, 4.00, 60, 40, ?, ?, 'prueba')"
            + " RETURNING id_liquidacion",
        Long.class,
        lote,
        galpon,
        estado,
        Timestamp.from(Instant.parse(generada)));
  }
}
