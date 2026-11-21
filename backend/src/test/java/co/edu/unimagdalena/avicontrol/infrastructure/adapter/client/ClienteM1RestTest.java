package co.edu.unimagdalena.avicontrol.infrastructure.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM1Exception;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CU07 (plan 001, T009): el cliente REST contra un M1 simulado con los JSON del acuerdo. */
class ClienteM1RestTest {

  private M1Simulado m1;
  private ClienteM1Rest cliente;

  @BeforeEach
  void arrancar() throws Exception {
    m1 = new M1Simulado();
    cliente = clienteConLectura(Duration.ofSeconds(2));
  }

  @AfterEach
  void detener() {
    m1.close();
  }

  private ClienteM1Rest clienteConLectura(Duration lectura) {
    return new ClienteM1Rest(m1.urlBase(), Duration.ofSeconds(1), lectura);
  }

  @Test
  void recorreTodasLasPaginasDeLaListaDeGalpones() {
    m1.responder(
            "/galpones?page=1",
            200,
            """
            {"contenido":[{"id":"%s","nombre":"galpon demo","capacidad":5000,"estado":"EN_COSECHA"}],
             "pagina":1,"tamanio":10,"totalElementos":2,"totalPaginas":2,"sinCoincidencias":false}
            """
                .formatted(M1Simulado.GALPON_UNO))
        .responder(
            "/galpones?page=2",
            200,
            """
            {"contenido":[{"id":"%s","nombre":"galpon demo 2","capacidad":5200,"estado":"DISPONIBLE"}],
             "pagina":2,"tamanio":10,"totalElementos":2,"totalPaginas":2,"sinCoincidencias":false}
            """
                .formatted(M1Simulado.GALPON_DOS));

    var galpones = cliente.listarGalpones();

    assertThat(galpones).hasSize(2);
    assertThat(galpones.get(0).nombre()).isEqualTo("galpon demo");
    assertThat(galpones.get(0).capacidad()).isEqualTo(5000L);
    assertThat(galpones.get(0).estado()).isEqualTo("EN_COSECHA");
    assertThat(galpones.get(0).loteActivo()).isNull();
    assertThat(galpones.get(1).id()).isEqualTo(UUID.fromString(M1Simulado.GALPON_DOS));
  }

  @Test
  void unaListaVaciaNoEsUnError() {
    m1.responder(
        "/galpones?page=1",
        200,
        "{\"contenido\":[],\"pagina\":1,\"tamanio\":10,\"totalElementos\":0,\"totalPaginas\":0}");

    assertThat(cliente.listarGalpones()).isEmpty();
  }

  @Test
  void elDetalleTraeElLoteActivoYUnGalponSinLoteLoOmite() {
    m1.conLosDatosDeLaDemo();

    var conLote = cliente.detalle(UUID.fromString(M1Simulado.GALPON_DOS)).orElseThrow();
    var sinLote = cliente.detalle(UUID.fromString(M1Simulado.GALPON_UNO)).orElseThrow();

    assertThat(conLote.loteActivo()).isNotNull();
    assertThat(conLote.loteActivo().nombre()).isEqualTo("lote demo 3");
    assertThat(conLote.loteActivo().fechaIngreso()).isEqualTo(LocalDate.parse("2026-11-19"));
    assertThat(conLote.loteActivo().poblacionInicial()).isEqualTo(1000L);
    assertThat(conLote.loteActivo().costoTotal()).isEqualTo(4_000_000L);
    assertThat(sinLote.loteActivo()).isNull(); // 200 sin el campo loteActivo
  }

  @Test
  void elHistorialTraeLosLotesAnteriores() {
    m1.conLosDatosDeLaDemo();

    var historial = cliente.historial(UUID.fromString(M1Simulado.GALPON_UNO)).orElseThrow();

    assertThat(historial.loteActivo()).isNull();
    assertThat(historial.lotesAnteriores())
        .singleElement()
        .satisfies(
            lote -> {
              assertThat(lote.id()).isEqualTo(UUID.fromString(M1Simulado.LOTE_ANTERIOR));
              assertThat(lote.poblacionActual()).isEqualTo(1974L);
              assertThat(lote.costoTotal()).isEqualTo(5_000_000L);
            });
  }

  @Test
  void unGalponInexistenteDevuelveVacioEnVezDeFallar() {
    var desconocido = UUID.randomUUID();

    assertThat(cliente.detalle(desconocido)).isEmpty();
    assertThat(cliente.historial(desconocido)).isEmpty();
  }

  @Test
  void ignoraLosCamposQueNoConoce() {
    m1.responder(
        "/galpones/" + M1Simulado.GALPON_UNO,
        200,
        """
        {"id":"%s","nombre":"galpon demo","capacidad":5000,"estado":"PRODUCTIVO",
         "mensaje":"nuevo","incidencias":[{"codigo":"CAMPO_NO_DISPONIBLE","mensaje":"x"}],"campoFuturo":1}
        """
            .formatted(M1Simulado.GALPON_UNO));

    assertThat(cliente.detalle(UUID.fromString(M1Simulado.GALPON_UNO))).isPresent();
  }

  @Test
  void unErrorDeM1SeConvierteEnConsultaFallida() {
    m1.responder(
        "/galpones?page=1",
        500,
        "{\"status\":500,\"code\":\"ERROR_CONSULTA\",\"title\":\"Error técnico\"}");

    assertThatThrownBy(() -> cliente.listarGalpones())
        .isInstanceOf(ConsultaM1Exception.class)
        .hasMessageContaining("/galpones");
  }

  @Test
  void unParametroInvalidoSeConvierteEnConsultaFallida() {
    m1.responder(
        "/galpones/" + M1Simulado.GALPON_UNO,
        400,
        "{\"status\":400,\"code\":\"PARAMETRO_INVALIDO\",\"title\":\"Parámetro inválido\"}");

    assertThatThrownBy(() -> cliente.detalle(UUID.fromString(M1Simulado.GALPON_UNO)))
        .isInstanceOf(ConsultaM1Exception.class);
  }

  @Test
  void unaRespuestaQueNoEsJsonSeConvierteEnConsultaFallida() {
    m1.responder("/galpones?page=1", 200, "<html>no soy json</html>");

    assertThatThrownBy(() -> cliente.listarGalpones()).isInstanceOf(ConsultaM1Exception.class);
  }

  @Test
  void siM1TardaMasDelTiempoDeLecturaLaConsultaFalla() {
    m1.responder("/galpones?page=1", 200, "{\"contenido\":[],\"totalPaginas\":1}", 1500);
    var impaciente = clienteConLectura(Duration.ofMillis(300));

    assertThatThrownBy(impaciente::listarGalpones).isInstanceOf(ConsultaM1Exception.class);
  }

  @Test
  void siM1NoEstaLevantadoLaConsultaFalla() {
    String url = m1.urlBase();
    m1.close();
    var sinServidor = new ClienteM1Rest(url, Duration.ofMillis(500), Duration.ofSeconds(1));

    assertThatThrownBy(sinServidor::listarGalpones).isInstanceOf(ConsultaM1Exception.class);
  }
}
