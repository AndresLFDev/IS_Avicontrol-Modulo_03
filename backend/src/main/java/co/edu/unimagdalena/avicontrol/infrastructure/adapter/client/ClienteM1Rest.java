package co.edu.unimagdalena.avicontrol.infrastructure.adapter.client;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM1Exception;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.GalponM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.HistorialLotesM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteM1;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConsultarGalponesM1;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente REST de Módulo 1 (CU07). Usa los nombres de campo del acuerdo de contratos de M1 y los
 * traduce a los DTOs de M3. Tiempos máximos de conexión y lectura, y sin reintentos dentro de una
 * misma consulta: si M1 falla, la sincronización queda {@code FALLIDA} y el siguiente ciclo
 * reintenta.
 */
@Component
public class ClienteM1Rest implements ConsultarGalponesM1 {

  /** Tope de páginas por consulta: protege contra un {@code totalPaginas} absurdo. */
  private static final int MAXIMO_PAGINAS = 1000;

  private final RestClient http;

  public ClienteM1Rest(
      @Value("${m1.base-url:}") String urlBase,
      @Value("${m1.timeout-conexion:PT2S}") Duration timeoutConexion,
      @Value("${m1.timeout-lectura:PT5S}") Duration timeoutLectura) {
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
  public List<GalponM1> listarGalpones() {
    List<GalponM1> galpones = new ArrayList<>();
    int pagina = 1;
    int totalPaginas = 1;
    while (pagina <= totalPaginas && pagina <= MAXIMO_PAGINAS) {
      PaginaGalponesResponse respuesta =
          obtener("/galpones?page={pagina}", PaginaGalponesResponse.class, pagina)
              .orElseThrow(
                  () -> new ConsultaM1Exception("M1 respondió 404 a la lista de galpones"));
      if (respuesta.contenido() != null) {
        respuesta.contenido().forEach(g -> galpones.add(g.aGalpon()));
      }
      totalPaginas = Math.max(respuesta.totalPaginas(), 1);
      pagina++;
    }
    return galpones;
  }

  @Override
  public Optional<GalponM1> detalle(UUID idGalpon) {
    return obtener("/galpones/{id}", GalponResponse.class, idGalpon).map(GalponResponse::aGalpon);
  }

  @Override
  public Optional<HistorialLotesM1> historial(UUID idGalpon) {
    return obtener("/galpones/{id}/lotes", HistorialResponse.class, idGalpon)
        .map(HistorialResponse::aHistorial);
  }

  /**
   * GET que devuelve vacío ante {@code 404} y lanza {@link ConsultaM1Exception} ante todo error.
   */
  private <T> Optional<T> obtener(String ruta, Class<T> tipo, Object variable) {
    try {
      return Optional.ofNullable(http.get().uri(ruta, variable).retrieve().body(tipo));
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    } catch (RestClientException e) {
      // Cubre HTTP 400 y 500 (problem+json con su code), tiempo agotado y M1 sin respuesta
      throw new ConsultaM1Exception("Consulta a M1 fallida (" + ruta + "): " + e.getMessage(), e);
    }
  }

  // --- Respuestas de M1 (acuerdo de contratos). Los campos desconocidos se ignoran. ---

  @JsonIgnoreProperties(ignoreUnknown = true)
  record PaginaGalponesResponse(List<GalponResumenResponse> contenido, int totalPaginas) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GalponResumenResponse(UUID id, String nombre, Long capacidad, String estado) {
    GalponM1 aGalpon() {
      return new GalponM1(id, nombre, capacidad, estado, null);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GalponResponse(
      UUID id, String nombre, Long capacidad, String estado, LoteResponse loteActivo) {
    GalponM1 aGalpon() {
      return new GalponM1(
          id, nombre, capacidad, estado, loteActivo == null ? null : loteActivo.aLote());
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record HistorialResponse(
      UUID galponId, LoteResponse loteActivo, List<LoteResponse> lotesAnteriores) {
    HistorialLotesM1 aHistorial() {
      List<LoteM1> anteriores =
          lotesAnteriores == null
              ? List.of()
              : lotesAnteriores.stream().map(LoteResponse::aLote).toList();
      return new HistorialLotesM1(
          galponId, loteActivo == null ? null : loteActivo.aLote(), anteriores);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record LoteResponse(
      UUID id,
      String nombre,
      LocalDate fechaIngreso,
      Long poblacionInicial,
      Long poblacionActual,
      Long costoTotal) {
    LoteM1 aLote() {
      return new LoteM1(id, nombre, fechaIngreso, poblacionInicial, poblacionActual, costoTotal);
    }
  }
}
