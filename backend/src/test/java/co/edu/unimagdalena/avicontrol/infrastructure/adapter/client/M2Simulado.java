package co.edu.unimagdalena.avicontrol.infrastructure.adapter.client;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor HTTP que hace de Módulo 2 en las pruebas. Responde con la misma forma de JSON que
 * documenta M2 en {@code docs/integracion-modulo3-rest-kafka.md}. Cada método y ruta se registra
 * con {@link #responder}; lo no registrado responde {@code 404} sin el código de M2 (como una ruta
 * que no existe). Guarda cada petición recibida para poder comprobar qué envió M3.
 */
public final class M2Simulado implements AutoCloseable {

  public static final String LOTE = "7331eec8-e447-4868-bdef-8eca3fff8d44";
  public static final String BASE = "/api/integracion/modulo3";

  /** Petición recibida: método, ruta, cuerpo y si traía cabecera Authorization. */
  public record Peticion(String metodo, String ruta, String cuerpo, boolean conAutorizacion) {}

  private record Respuesta(int estado, String cuerpo, long demoraMs) {}

  private final HttpServer servidor;
  private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
  private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();

  public M2Simulado() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext(
        "/",
        intercambio -> {
          String metodo = intercambio.getRequestMethod();
          String ruta = intercambio.getRequestURI().getPath();
          String cuerpoPeticion =
              new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          peticiones.add(
              new Peticion(
                  metodo,
                  ruta,
                  cuerpoPeticion,
                  intercambio.getRequestHeaders().containsKey("Authorization")));
          var respuesta =
              respuestas.getOrDefault(
                  metodo + " " + ruta, new Respuesta(404, "{\"status\":404}", 0));
          try {
            Thread.sleep(respuesta.demoraMs());
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          byte[] cuerpo = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
          intercambio
              .getResponseHeaders()
              .add(
                  "Content-Type",
                  respuesta.estado() >= 400 ? "application/problem+json" : "application/json");
          intercambio.sendResponseHeaders(respuesta.estado(), cuerpo.length);
          intercambio.getResponseBody().write(cuerpo);
          intercambio.close();
        });
    servidor.start();
  }

  /** URL base equivalente a {@code M2_BASE_URL}. */
  public String urlBase() {
    return "http://127.0.0.1:" + servidor.getAddress().getPort() + BASE;
  }

  public List<Peticion> peticiones() {
    return peticiones;
  }

  public M2Simulado responder(
      String metodo, String rutaDespuesDeLaBase, int estado, String cuerpo) {
    return responder(metodo, rutaDespuesDeLaBase, estado, cuerpo, 0);
  }

  public M2Simulado responder(
      String metodo, String rutaDespuesDeLaBase, int estado, String cuerpo, long demoraMs) {
    respuestas.put(
        metodo + " " + BASE + rutaDespuesDeLaBase, new Respuesta(estado, cuerpo, demoraMs));
    return this;
  }

  public void limpiar() {
    respuestas.clear();
    peticiones.clear();
  }

  /** 404 que M2 da cuando el lote aún no tiene resultado final. */
  public M2Simulado sinResultado(String idLote) {
    return responder(
        "GET",
        "/lotes/" + idLote + "/resultado-sacrificio",
        404,
        """
        {"type":"https://avicontrol/errors/recurso-no-encontrado","title":"Not Found","status":404,
         "detail":"Resultado no encontrado para el lote","code":"RECURSO_NO_ENCONTRADO","fieldErrors":[]}
        """);
  }

  public M2Simulado conResultado(String idLote, String idResultado, int pollos, String pesoKg) {
    return responder(
        "GET",
        "/lotes/" + idLote + "/resultado-sacrificio",
        200,
        """
        {"id":"%s","ordenId":"11111111-1111-4111-8111-111111111111",
         "galponId":"a402a368-cd4d-4e27-8d48-9e0d7eb8d374","loteId":"%s",
         "cantidadFinalPollos":%d,"pesoTotalKg":%s,"estado":"REGISTRADO",
         "fechaRegistro":"2026-11-20T18:30:00Z","fechaCorreccion":null}
        """
            .formatted(idResultado, idLote, pollos, pesoKg));
  }

  /** Alimento de un lote: una partida de 2 kg a 10 COP/kg con 19 %, como el ejemplo de M2. */
  public M2Simulado conAlimentos(String idLote, String cuerpoPartidas) {
    return responder(
        "GET",
        "/lotes/" + idLote + "/alimentos",
        200,
        "{\"loteId\":\"" + idLote + "\",\"partidas\":" + cuerpoPartidas + "}");
  }

  public M2Simulado conMedicamentos(String idLote, String cuerpoPartidas) {
    return responder(
        "GET",
        "/lotes/" + idLote + "/medicamentos",
        200,
        "{\"loteId\":\"" + idLote + "\",\"partidas\":" + cuerpoPartidas + "}");
  }

  @Override
  public void close() {
    servidor.stop(0);
  }
}
