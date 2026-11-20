package co.edu.unimagdalena.avicontrol.infrastructure.adapter.client;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor HTTP que hace de Módulo 1 en las pruebas. Responde con los mismos JSON del acuerdo de
 * contratos de M1 (y con los que devuelve su servicio desplegado). Cada ruta con su consulta se
 * registra con {@link #responder}; lo no registrado responde {@code 404}.
 */
public final class M1Simulado implements AutoCloseable {

  public static final String GALPON_UNO = "a402a368-cd4d-4e27-8d48-9e0d7eb8d374";
  public static final String GALPON_DOS = "ec8e8c65-812c-405f-8c13-58fad384111d";
  public static final String LOTE_ANTERIOR = "7331eec8-e447-4868-bdef-8eca3fff8d44";
  public static final String LOTE_ACTIVO = "2f545f14-128e-4e02-8b74-f00925789dc5";

  private record Respuesta(int estado, String cuerpo, long demoraMs) {}

  private final HttpServer servidor;
  private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
  private final AtomicInteger consultas = new AtomicInteger();

  public M1Simulado() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext(
        "/",
        intercambio -> {
          consultas.incrementAndGet();
          var uri = intercambio.getRequestURI();
          String clave = uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery());
          var respuesta =
              respuestas.getOrDefault(
                  clave,
                  new Respuesta(
                      404,
                      "{\"status\":404,\"code\":\"GALPON_NO_ENCONTRADO\",\"title\":\"no existe\"}",
                      0));
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

  /** URL base equivalente a {@code M1_BASE_URL}. */
  public String urlBase() {
    return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/api/v1";
  }

  public int consultas() {
    return consultas.get();
  }

  public M1Simulado responder(String rutaYConsulta, int estado, String cuerpo) {
    return responder(rutaYConsulta, estado, cuerpo, 0);
  }

  public M1Simulado responder(String rutaYConsulta, int estado, String cuerpo, long demoraMs) {
    respuestas.put("/api/v1" + rutaYConsulta, new Respuesta(estado, cuerpo, demoraMs));
    return this;
  }

  public void limpiar() {
    respuestas.clear();
    consultas.set(0);
  }

  /** El estado que tenía M1 en noviembre de 2026: tres galpones y un lote ya desvinculado. */
  public M1Simulado conLosDatosDeLaDemo() {
    responder(
        "/galpones?page=1",
        200,
        """
        {"contenido":[
          {"id":"%s","nombre":"galpon demo","capacidad":5000,"estado":"VACIADO_SANITARIO"},
          {"id":"%s","nombre":"galpón demo 3","capacidad":4500,"estado":"AISLAMIENTO"}],
         "pagina":1,"tamanio":10,"totalElementos":2,"totalPaginas":1,"sinCoincidencias":false}
        """
            .formatted(GALPON_UNO, GALPON_DOS));
    // Galpón en vaciado sanitario: sin loteActivo (M1 omite el campo, nunca envía null)
    responder(
        "/galpones/" + GALPON_UNO,
        200,
        """
        {"id":"%s","nombre":"galpon demo","capacidad":5000,"estado":"VACIADO_SANITARIO"}
        """
            .formatted(GALPON_UNO));
    responder(
        "/galpones/" + GALPON_UNO + "/lotes",
        200,
        """
        {"galponId":"%s","lotesAnteriores":[
          {"id":"%s","nombre":"lote demo","poblacionInicial":2000,"poblacionActual":1974,
           "fechaIngreso":"2026-11-18","edadDias":2,"costoTotal":5000000}]}
        """
            .formatted(GALPON_UNO, LOTE_ANTERIOR));
    responder(
        "/galpones/" + GALPON_DOS,
        200,
        """
        {"id":"%s","nombre":"galpón demo 3","capacidad":4500,"estado":"AISLAMIENTO",
         "loteActivo":{"id":"%s","nombre":"lote demo 3","poblacionInicial":1000,"poblacionActual":1000,
           "fechaIngreso":"2026-11-19","edadDias":2,"costoTotal":4000000}}
        """
            .formatted(GALPON_DOS, LOTE_ACTIVO));
    responder(
        "/galpones/" + GALPON_DOS + "/lotes",
        200,
        """
        {"galponId":"%s","loteActivo":{"id":"%s","nombre":"lote demo 3","poblacionInicial":1000,
           "poblacionActual":1000,"fechaIngreso":"2026-11-19","edadDias":2,"costoTotal":4000000},
         "lotesAnteriores":[]}
        """
            .formatted(GALPON_DOS, LOTE_ACTIVO));
    return this;
  }

  @Override
  public void close() {
    servidor.stop(0);
  }
}
