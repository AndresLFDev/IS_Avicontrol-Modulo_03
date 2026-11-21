package co.edu.unimagdalena.avicontrol.infrastructure.adapter.client;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM2Exception;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.PartidaAlimentoM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.PartidaMedicamentoM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResultadoM2;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConfirmarUtilizacionM2;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConsultarFinanzasM2;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente REST de Módulo 2 (CU08 a CU10 y confirmación de utilización). M2 autoriza a M3 por su IP
 * de salida: no se envía usuario, clave ni cabecera Authorization. La dirección base es la de la
 * integración con M3, por ejemplo {@code https://3.21.155.197/api/integracion/modulo3}. Tiempos
 * máximos de conexión y lectura, y sin reintentos dentro de una misma llamada: el siguiente ciclo
 * reintenta.
 */
@Component
public class ClienteM2Rest implements ConsultarFinanzasM2, ConfirmarUtilizacionM2 {

  static final String TIPO_EVENTO = "ResultadoSacrificioUtilizado";
  static final String ESTADO_CONFIRMADO = "UTILIZADO";
  private static final String CODIGO_NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO";

  private final RestClient http;

  public ClienteM2Rest(
      @Value("${m2.base-url:}") String urlBase,
      @Value("${m2.timeout-conexion:PT2S}") Duration timeoutConexion,
      @Value("${m2.timeout-lectura:PT5S}") Duration timeoutLectura) {
    var cliente = HttpClient.newBuilder().connectTimeout(timeoutConexion).build();
    var fabrica = new JdkClientHttpRequestFactory(cliente);
    fabrica.setReadTimeout(timeoutLectura);
    this.http =
        RestClient.builder()
            .baseUrl(urlBase)
            .requestFactory(fabrica)
            .defaultHeader("Accept", "application/json, application/problem+json")
            .build();
  }

  @Override
  public Optional<ResultadoM2> resultado(UUID idLote) {
    try {
      var r =
          http.get()
              .uri("/lotes/{id}/resultado-sacrificio", idLote)
              .retrieve()
              .body(ResultadoResponse.class);
      return Optional.ofNullable(r).map(ResultadoResponse::aResultado);
    } catch (HttpClientErrorException.NotFound e) {
      // Solo el 404 de M2 por "sin resultado" es normal; un 404 de otra ruta es un error
      if (e.getResponseBodyAsString().contains(CODIGO_NO_ENCONTRADO)) {
        return Optional.empty();
      }
      throw falla("/lotes/{id}/resultado-sacrificio", e);
    } catch (RestClientException e) {
      throw falla("/lotes/{id}/resultado-sacrificio", e);
    }
  }

  @Override
  public List<PartidaAlimentoM2> alimentos(UUID idLote) {
    try {
      var r =
          http.get().uri("/lotes/{id}/alimentos", idLote).retrieve().body(AlimentosResponse.class);
      if (r == null || r.partidas() == null) {
        return List.of();
      }
      return r.partidas().stream().map(AlimentoResponse::aPartida).toList();
    } catch (RestClientException e) {
      throw falla("/lotes/{id}/alimentos", e);
    }
  }

  @Override
  public List<PartidaMedicamentoM2> medicamentos(UUID idLote) {
    try {
      var r =
          http.get()
              .uri("/lotes/{id}/medicamentos", idLote)
              .retrieve()
              .body(MedicamentosResponse.class);
      if (r == null || r.partidas() == null) {
        return List.of();
      }
      return r.partidas().stream().map(MedicamentoResponse::aPartida).toList();
    } catch (RestClientException e) {
      throw falla("/lotes/{id}/medicamentos", e);
    }
  }

  @Override
  public void confirmar(UUID idEvento, UUID idResultado, UUID idLote) {
    var cuerpo =
        new AvisoRequest(
            idEvento, TIPO_EVENTO, 1, idResultado, new AvisoRequest.Payload(idResultado, idLote));
    try {
      var r =
          http.post()
              .uri("/resultados-sacrificio/utilizado")
              .contentType(MediaType.APPLICATION_JSON)
              .body(cuerpo)
              .retrieve()
              .body(ConfirmacionResponse.class);
      // Solo un 200 con el resultado confirmado cuenta: M2 ya bloqueó su corrección
      if (r == null
          || !ESTADO_CONFIRMADO.equals(r.estado())
          || !idResultado.equals(r.resultadoId())) {
        throw new ConsultaM2Exception("M2 respondió sin confirmar el resultado " + idResultado);
      }
    } catch (RestClientException e) {
      throw falla("/resultados-sacrificio/utilizado", e);
    }
  }

  private static ConsultaM2Exception falla(String ruta, RestClientException e) {
    // Cubre HTTP 400, 403, 404, 409 y 500, el tiempo agotado y M2 sin respuesta
    return new ConsultaM2Exception("Llamada a M2 fallida (" + ruta + "): " + e.getMessage(), e);
  }

  // --- Respuestas de M2. Los campos desconocidos se ignoran (M2 puede agregar los suyos). ---

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ResultadoResponse(
      UUID id,
      UUID loteId,
      UUID galponId,
      Integer cantidadFinalPollos,
      BigDecimal pesoTotalKg,
      Instant fechaRegistro) {
    ResultadoM2 aResultado() {
      return new ResultadoM2(id, loteId, galponId, cantidadFinalPollos, pesoTotalKg, fechaRegistro);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record AlimentosResponse(UUID loteId, List<AlimentoResponse> partidas) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record AlimentoResponse(
      UUID suministroId,
      UUID recepcionId,
      String nombreAlimento,
      String tipoAlimento,
      BigDecimal cantidadKg,
      BigDecimal precioNetoKg,
      BigDecimal valorImpuestoCop) {
    PartidaAlimentoM2 aPartida() {
      return new PartidaAlimentoM2(
          suministroId,
          recepcionId,
          nombreAlimento,
          tipoAlimento,
          cantidadKg,
          precioNetoKg,
          valorImpuestoCop);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record MedicamentosResponse(UUID loteId, List<MedicamentoResponse> partidas) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record MedicamentoResponse(
      UUID consumoId,
      UUID recepcionId,
      UUID medicamentoId,
      String nombreMedicamento,
      LocalDate fechaAplicacion,
      BigDecimal cantidadUnidadBase,
      String unidadBase,
      BigDecimal precioNetoHistoricoUnidadBase,
      BigDecimal valorImpuestoCop) {
    PartidaMedicamentoM2 aPartida() {
      return new PartidaMedicamentoM2(
          consumoId,
          recepcionId,
          medicamentoId,
          nombreMedicamento,
          fechaAplicacion,
          cantidadUnidadBase,
          unidadBase,
          precioNetoHistoricoUnidadBase,
          valorImpuestoCop);
    }
  }

  /** Sobre del aviso, tal como lo define M2 (todos los campos son obligatorios). */
  record AvisoRequest(
      UUID eventId, String eventType, int eventVersion, UUID aggregateId, Payload payload) {
    record Payload(UUID resultadoId, UUID loteId) {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ConfirmacionResponse(UUID eventId, UUID resultadoId, UUID loteId, String estado) {}
}
