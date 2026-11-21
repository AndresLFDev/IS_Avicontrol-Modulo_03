package co.edu.unimagdalena.avicontrol.infrastructure.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM2Exception;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El cliente REST de M2 contra un M2 simulado con los JSON que documenta M2. */
class ClienteM2RestTest {

  private static final UUID LOTE = UUID.fromString(M2Simulado.LOTE);
  private static final String RESULTADO = "22222222-2222-4222-8222-222222222222";
  private static final String SUMINISTRO = "5a000000-0000-4000-8000-000000000001";
  private static final String RECEPCION_1 = "6b000000-0000-4000-8000-000000000001";
  private static final String RECEPCION_2 = "6b000000-0000-4000-8000-000000000002";
  private static final String CONSUMO = "7c000000-0000-4000-8000-000000000001";
  private static final String MEDICAMENTO = "8d000000-0000-4000-8000-000000000001";

  private M2Simulado m2;
  private ClienteM2Rest cliente;

  @BeforeEach
  void arrancar() throws Exception {
    m2 = new M2Simulado();
    cliente = new ClienteM2Rest(m2.urlBase(), Duration.ofSeconds(1), Duration.ofSeconds(2));
  }

  @AfterEach
  void detener() {
    m2.close();
  }

  // --- Resultado ---

  @Test
  void leeElResultadoFinalDelLote() {
    m2.conResultado(M2Simulado.LOTE, RESULTADO, 1974, "4012.50");

    var resultado = cliente.resultado(LOTE).orElseThrow();

    assertThat(resultado.id()).isEqualTo(UUID.fromString(RESULTADO));
    assertThat(resultado.loteId()).isEqualTo(LOTE);
    assertThat(resultado.cantidadFinalPollos()).isEqualTo(1974);
    assertThat(resultado.pesoTotalKg()).isEqualByComparingTo("4012.50");
    assertThat(resultado.fechaRegistro()).isEqualTo(Instant.parse("2026-11-20T18:30:00Z"));
  }

  @Test
  void sinResultadoEnM2NoEsUnError() {
    m2.sinResultado(M2Simulado.LOTE);

    assertThat(cliente.resultado(LOTE)).isEmpty();
  }

  @Test
  void unNotFoundSinElCodigoDeM2EsUnErrorYNoUnLoteSinResultado() {
    // Sin ruta registrada: el 404 no trae RECURSO_NO_ENCONTRADO (p. ej. M2 sin la versión nueva)
    assertThatThrownBy(() -> cliente.resultado(LOTE)).isInstanceOf(ConsultaM2Exception.class);
  }

  @Test
  void noEnviaCabeceraDeAutorizacion() {
    m2.sinResultado(M2Simulado.LOTE);

    cliente.resultado(LOTE);

    assertThat(m2.peticiones()).hasSize(1);
    assertThat(m2.peticiones().get(0).conAutorizacion()).isFalse();
    assertThat(m2.peticiones().get(0).ruta())
        .isEqualTo(M2Simulado.BASE + "/lotes/" + M2Simulado.LOTE + "/resultado-sacrificio");
  }

  // --- Alimento ---

  @Test
  void leeLasPartidasDeAlimentoSeparadasPorRecepcion() {
    m2.conAlimentos(
        M2Simulado.LOTE,
        """
        [{"suministroId":"%s","recepcionId":"%s","galponId":"a402a368-cd4d-4e27-8d48-9e0d7eb8d374",
          "alimentoId":"9e000000-0000-4000-8000-000000000001","nombreAlimento":"Inicio Premium",
          "tipoAlimento":"INICIO","fechaAplicacion":"2026-11-10","cantidadKg":2.0000,
          "precioNetoKg":10.0000,"impuestoPorcentaje":19.0000,"valorImpuestoCop":3.80,
          "valorImpuestoRecepcionCop":190.00,"moneda":"COP","datosCompletos":true,"campoNuevo":"x"},
         {"suministroId":"%s","recepcionId":"%s","nombreAlimento":"Inicio Premium",
          "tipoAlimento":"INICIO","cantidadKg":1.5000,"precioNetoKg":11.2500,
          "valorImpuestoCop":3.21,"datosCompletos":true}]
        """
            .formatted(SUMINISTRO, RECEPCION_1, SUMINISTRO, RECEPCION_2));

    var partidas = cliente.alimentos(LOTE);

    assertThat(partidas).hasSize(2);
    assertThat(partidas.get(0).suministroId()).isEqualTo(UUID.fromString(SUMINISTRO));
    assertThat(partidas.get(0).recepcionId()).isEqualTo(UUID.fromString(RECEPCION_1));
    assertThat(partidas.get(0).tipoAlimento()).isEqualTo("INICIO");
    assertThat(partidas.get(0).cantidadKg()).isEqualByComparingTo("2");
    assertThat(partidas.get(0).precioNetoKg()).isEqualByComparingTo("10");
    assertThat(partidas.get(0).valorImpuestoCop()).isEqualByComparingTo("3.80");
    // Mismo suministro, otra recepción, otro precio: no se consolidan
    assertThat(partidas.get(1).recepcionId()).isEqualTo(UUID.fromString(RECEPCION_2));
    assertThat(partidas.get(1).precioNetoKg()).isEqualByComparingTo(new BigDecimal("11.25"));
  }

  @Test
  void unLoteSinAlimentoDevuelveListaVacia() {
    m2.conAlimentos(M2Simulado.LOTE, "[]");

    assertThat(cliente.alimentos(LOTE)).isEmpty();
  }

  // --- Medicamento ---

  @Test
  void leeLosTramosDeMedicamentoUnoPorRecepcion() {
    m2.conMedicamentos(
        M2Simulado.LOTE,
        """
        [{"consumoId":"%s","recepcionId":"%s","galponId":"a402a368-cd4d-4e27-8d48-9e0d7eb8d374",
          "medicamentoId":"%s","nombreMedicamento":"Enrofloxacina","fechaAplicacion":"2026-11-12",
          "cantidadUnidadBase":120.5000,"unidadBase":"ML","precioNetoHistoricoUnidadBase":0.0333,
          "impuestoPorcentaje":19.0000,"valorImpuestoCop":0.76,"valorImpuestoRecepcionCop":50.00,
          "moneda":"COP","datosCompletos":true}]
        """
            .formatted(CONSUMO, RECEPCION_1, MEDICAMENTO));

    var tramos = cliente.medicamentos(LOTE);

    assertThat(tramos).hasSize(1);
    assertThat(tramos.get(0).consumoId()).isEqualTo(UUID.fromString(CONSUMO));
    assertThat(tramos.get(0).nombreMedicamento()).isEqualTo("Enrofloxacina");
    assertThat(tramos.get(0).fechaAplicacion()).isEqualTo(LocalDate.of(2026, 11, 12));
    assertThat(tramos.get(0).unidadBase()).isEqualTo("ML");
    // Los 4 decimales del precio llegan completos
    assertThat(tramos.get(0).precioNetoUnidadBase()).isEqualByComparingTo("0.0333");
  }

  // --- Errores ---

  @Test
  void unErrorDelServidorEsConsultaM2Exception() {
    m2.responder(
        "GET",
        "/lotes/" + M2Simulado.LOTE + "/alimentos",
        500,
        "{\"status\":500,\"code\":\"ERROR_INTERNO\"}");

    assertThatThrownBy(() -> cliente.alimentos(LOTE))
        .isInstanceOf(ConsultaM2Exception.class)
        .hasMessageContaining("alimentos");
  }

  @Test
  void unM2QueNoRespondeATiempoEsConsultaM2Exception() {
    m2.conMedicamentos(M2Simulado.LOTE, "[]");
    m2.responder("GET", "/lotes/" + M2Simulado.LOTE + "/medicamentos", 200, "{}", 3000);

    assertThatThrownBy(() -> cliente.medicamentos(LOTE)).isInstanceOf(ConsultaM2Exception.class);
  }

  @Test
  void unM2ApagadoEsConsultaM2Exception() {
    m2.close();

    assertThatThrownBy(() -> cliente.alimentos(LOTE)).isInstanceOf(ConsultaM2Exception.class);
  }

  // --- Confirmación de utilización ---

  @Test
  void confirmaLaUtilizacionConElSobreQueExigeM2() {
    m2.responder(
        "POST",
        "/resultados-sacrificio/utilizado",
        200,
        """
        {"eventId":"11111111-1111-4111-8111-111111111111","resultadoId":"%s","loteId":"%s",
         "estado":"UTILIZADO"}
        """
            .formatted(RESULTADO, M2Simulado.LOTE));

    cliente.confirmar(
        UUID.fromString("11111111-1111-4111-8111-111111111111"), UUID.fromString(RESULTADO), LOTE);

    var envio = m2.peticiones().get(0);
    assertThat(envio.metodo()).isEqualTo("POST");
    assertThat(envio.ruta()).isEqualTo(M2Simulado.BASE + "/resultados-sacrificio/utilizado");
    assertThat(envio.conAutorizacion()).isFalse();
    assertThat(envio.cuerpo())
        .contains("\"eventId\":\"11111111-1111-4111-8111-111111111111\"")
        .contains("\"eventType\":\"ResultadoSacrificioUtilizado\"")
        .contains("\"eventVersion\":1")
        .contains("\"aggregateId\":\"" + RESULTADO + "\"")
        .contains("\"resultadoId\":\"" + RESULTADO + "\"")
        .contains("\"loteId\":\"" + M2Simulado.LOTE + "\"");
  }

  @Test
  void unConflictoDeM2NoSeTomaComoConfirmado() {
    m2.responder(
        "POST", "/resultados-sacrificio/utilizado", 409, "{\"status\":409,\"code\":\"CONFLICTO\"}");

    assertThatThrownBy(() -> cliente.confirmar(UUID.randomUUID(), UUID.fromString(RESULTADO), LOTE))
        .isInstanceOf(ConsultaM2Exception.class);
  }

  @Test
  void unaRespuestaQueNoDiceUtilizadoNoCuentaComoConfirmada() {
    m2.responder(
        "POST",
        "/resultados-sacrificio/utilizado",
        200,
        """
        {"eventId":"11111111-1111-4111-8111-111111111111","resultadoId":"%s","loteId":"%s",
         "estado":"PENDIENTE"}
        """
            .formatted(RESULTADO, M2Simulado.LOTE));

    assertThatThrownBy(() -> cliente.confirmar(UUID.randomUUID(), UUID.fromString(RESULTADO), LOTE))
        .isInstanceOf(ConsultaM2Exception.class)
        .hasMessageContaining("sin confirmar");
  }

  @Test
  void unaConfirmacionDeOtroResultadoNoCuenta() {
    m2.responder(
        "POST",
        "/resultados-sacrificio/utilizado",
        200,
        """
        {"eventId":"11111111-1111-4111-8111-111111111111",
         "resultadoId":"99999999-9999-4999-8999-999999999999","loteId":"%s","estado":"UTILIZADO"}
        """
            .formatted(M2Simulado.LOTE));

    assertThatThrownBy(() -> cliente.confirmar(UUID.randomUUID(), UUID.fromString(RESULTADO), LOTE))
        .isInstanceOf(ConsultaM2Exception.class);
  }
}
