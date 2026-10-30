package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import co.edu.unimagdalena.avicontrol.TestcontainersConfiguration;
import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.model.LoteListado;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Plan 002, T005: derivación de la etapa en SQL (M3-CU01.FR-013), exclusión de los lotes liquidados
 * (FR-014), filtro, búsqueda y total de la paginación contra PostgreSQL real.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class LoteJdbcAdapterIT {

  @Autowired private LoteJdbcAdapter adaptador;
  @Autowired private JdbcTemplate jdbc;

  private long idRegistro;

  @BeforeEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE registro_anulacion, snapshot_datos_origen, partida_costo_lote, liquidacion,"
            + " aviso_utilizacion_resultado, resultado_final_sacrificio, alerta_vaciado_sanitario,"
            + " lote, galpon, registro_sincronizacion RESTART IDENTITY CASCADE");
    idRegistro =
        jdbc.queryForObject(
            "INSERT INTO registro_sincronizacion (fuente, topico, fecha_hora_inicio, resultado)"
                + " VALUES ('MODULO_1', 'avicontrol.m1.alertas-vaciado', now(), 'EXITOSA')"
                + " RETURNING id",
            Long.class);
  }

  @Test
  void derivaLaEtapaDeCadaLoteYOmiteLosNoListables() {
    lote("Norte", "PRODUCTIVO", "L-01", 9420);
    lote("Sur", "EN_COSECHA", "L-02", 9550);
    lote("Central", "AISLAMIENTO", "L-03", 7610);
    UUID porLiquidar = lote("Este", "VACIADO_SANITARIO", "L-04", 8500);
    alerta(porLiquidar);
    resultado(porLiquidar);
    lote("Mantenimiento", "MANTENIMIENTO", "L-05", 100); // sin alerta ni etapa → no se lista
    galpon("Río", "DISPONIBLE"); // galpón sin lote → no genera fila

    var pagina = adaptador.buscarListables(null, null, 0, 6);

    assertThat(pagina.totalElements()).isEqualTo(4);
    assertThat(pagina.contenido())
        .extracting(LoteListado::nombreLote, LoteListado::etapa)
        .containsExactly(
            tuple("L-01", EtapaLote.PRODUCTIVO),
            tuple("L-02", EtapaLote.EN_COSECHA),
            tuple("L-03", EtapaLote.AISLAMIENTO),
            tuple("L-04", EtapaLote.POR_LIQUIDAR));
    assertThat(pagina.contenido().get(3).tieneResultadoSacrificio()).isTrue();
    assertThat(adaptador.existeAlgunoListable()).isTrue();
  }

  @Test
  void excluyeElLoteConLiquidacionActivaYLoDevuelveTrasAnularla() {
    UUID lote = lote("Este", "VACIADO_SANITARIO", "L-04", 8500);
    alerta(lote);
    long liquidacion = liquidacion(lote, "ACTIVA");

    assertThat(adaptador.buscarListables(null, null, 0, 6).totalElements()).isZero();
    assertThat(adaptador.existeAlgunoListable()).isFalse();

    jdbc.update("UPDATE liquidacion SET estado = 'ANULADA' WHERE id_liquidacion = ?", liquidacion);

    var pagina = adaptador.buscarListables(null, null, 0, 6);
    assertThat(pagina.contenido())
        .singleElement()
        .extracting(LoteListado::etapa)
        .isEqualTo(EtapaLote.POR_LIQUIDAR);
  }

  @Test
  void elLoteAnteriorDeUnGalponReocupadoSigueSiendoPorLiquidar() {
    UUID galpon = galpon("Este", "PRODUCTIVO");
    UUID anterior = loteEn(galpon, "L-viejo", 0);
    alerta(anterior);
    loteEn(galpon, "L-nuevo", 9000);

    var pagina = adaptador.buscarListables(null, null, 0, 6);

    assertThat(pagina.contenido())
        .extracting(LoteListado::nombreLote, LoteListado::etapa)
        .containsExactlyInAnyOrder(
            tuple("L-viejo", EtapaLote.POR_LIQUIDAR), tuple("L-nuevo", EtapaLote.PRODUCTIVO));
  }

  @Test
  void filtraPorEtapa() {
    lote("Norte", "PRODUCTIVO", "L-01", 9420);
    lote("Sur", "EN_COSECHA", "L-02", 9550);

    var pagina = adaptador.buscarListables(EtapaLote.EN_COSECHA, null, 0, 6);

    assertThat(pagina.contenido()).extracting(LoteListado::nombreLote).containsExactly("L-02");
    assertThat(pagina.totalElements()).isEqualTo(1);
  }

  @Test
  void buscaPorNombreDeLoteDeGalponYPorUuid() {
    UUID norte = lote("Norte", "PRODUCTIVO", "L-01", 9420);
    lote("Sur", "EN_COSECHA", "L-02", 9550);

    assertThat(adaptador.buscarListables(null, "l-02", 0, 6).contenido())
        .extracting(LoteListado::nombreLote)
        .containsExactly("L-02");
    assertThat(adaptador.buscarListables(null, "galpón nor", 0, 6).contenido())
        .extracting(LoteListado::nombreLote)
        .containsExactly("L-01");
    assertThat(adaptador.buscarListables(null, norte.toString().toUpperCase(), 0, 6).contenido())
        .extracting(LoteListado::idLote)
        .containsExactly(norte);
  }

  @Test
  void losComodinesDeLaBusquedaSonLiterales() {
    lote("Norte", "PRODUCTIVO", "L-01", 9420);

    assertThat(adaptador.buscarListables(null, "%", 0, 6).totalElements()).isZero();
    assertThat(adaptador.buscarListables(null, "L_01", 0, 6).totalElements()).isZero();
  }

  @Test
  void paginaConElTotalRealDelFiltro() {
    for (int i = 1; i <= 7; i++) {
      lote("G" + i, "PRODUCTIVO", "L-0" + i, 1000);
    }

    var primera = adaptador.buscarListables(null, null, 0, 6);
    var segunda = adaptador.buscarListables(null, null, 1, 6);
    var fuera = adaptador.buscarListables(null, null, 4, 6);

    assertThat(primera.contenido()).hasSize(6);
    assertThat(segunda.contenido()).extracting(LoteListado::nombreLote).containsExactly("L-07");
    assertThat(fuera.contenido()).isEmpty();
    assertThat(fuera.totalElements()).isEqualTo(7);
    assertThat(fuera.totalPages()).isEqualTo(2);
  }

  private UUID galpon(String nombre, String estado) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO galpon (id_galpon, nombre, aforo_maximo, estado, fecha_hora_sync)"
            + " VALUES (?, ?, 10000, ?, now())",
        id,
        "Galpón " + nombre,
        estado);
    return id;
  }

  private UUID lote(String galpon, String estadoGalpon, String nombre, int poblacionActual) {
    return loteEn(galpon(galpon, estadoGalpon), nombre, poblacionActual);
  }

  private UUID loteEn(UUID galpon, String nombre, int poblacionActual) {
    UUID id = UUID.randomUUID();
    // Fecha de ingreso creciente según el último dígito del nombre, para fijar el orden de la lista
    char ultimo = nombre.charAt(nombre.length() - 1);
    int numero = Character.isDigit(ultimo) ? ultimo - '0' : 0;
    jdbc.update(
        "INSERT INTO lote (id_lote, id_galpon, nombre, fecha_ingreso, poblacion_inicial,"
            + " poblacion_actual, costo_total_cop, estado, fecha_hora_sync)"
            + " VALUES (?, ?, ?, DATE '2026-08-01' + ?, 10000, ?, 20000000, 'ACTIVO', now())",
        id,
        galpon,
        nombre,
        numero,
        poblacionActual);
    return id;
  }

  private void alerta(UUID lote) {
    jdbc.update(
        "INSERT INTO alerta_vaciado_sanitario (id_alerta, id_galpon, id_lote, fecha_hora_evento,"
            + " id_registro_sincronizacion)"
            + " SELECT ?, id_galpon, id_lote, now(), ? FROM lote WHERE id_lote = ?",
        UUID.randomUUID(),
        idRegistro,
        lote);
  }

  private void resultado(UUID lote) {
    jdbc.update(
        "INSERT INTO resultado_final_sacrificio (id_resultado, id_lote, id_galpon,"
            + " cantidad_final_pollos, peso_total_kg, fecha_registro, fecha_hora_sync)"
            + " SELECT ?, id_lote, id_galpon, 8500, 23800.00, now(), now() FROM lote WHERE id_lote = ?",
        UUID.randomUUID(),
        lote);
  }

  private long liquidacion(UUID lote, String estado) {
    return jdbc.queryForObject(
        "INSERT INTO liquidacion (id_lote, id_galpon, pollos_vendidos, peso_total_kg,"
            + " venta_bruta_cop, mortalidad_aves, porcentaje_mortalidad, costos_operativos_cop,"
            + " utilidad_neta_cop, estado, fecha_hora_generacion, usuario_responsable)"
            + " SELECT id_lote, id_galpon, 0, 0, 0, 0, 0, 0, 0, ?, now(), 'prueba'"
            + " FROM lote WHERE id_lote = ? RETURNING id_liquidacion",
        Long.class,
        estado,
        lote);
  }
}
