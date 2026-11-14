package co.edu.unimagdalena.avicontrol.infrastructure.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * CORS del frontend (Vite, otro origen) hacia {@code /api/**}. Se usa la configuración real de
 * {@link CorsConfig} sobre un contexto web mínimo con un controlador de sonda, para comprobar lo
 * que el navegador exige antes de un {@code PUT} (la anulación) sin levantar PostgreSQL ni Kafka.
 */
@SpringJUnitWebConfig(classes = {CorsConfig.class, CorsConfigTest.Sonda.class})
@TestPropertySource(properties = "cors.origenes-permitidos=http://localhost:5173")
class CorsConfigTest {

  private static final String ORIGEN_PERMITIDO = "http://localhost:5173";
  private static final String ORIGEN_AJENO = "http://sitio-ajeno.example";

  private MockMvc mvc;

  @BeforeEach
  void configurar(WebApplicationContext contexto) {
    mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
  }

  @Test
  void elPreflightDelFrontendParaPutEsAceptado() throws Exception {
    mvc.perform(
            options("/api/v1/sonda")
                .header(ORIGIN, ORIGEN_PERMITIDO)
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
        .andExpect(status().isOk())
        .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_PERMITIDO))
        .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")));
  }

  @Test
  void unaPeticionRealDelFrontendRecibeElEncabezadoCors() throws Exception {
    mvc.perform(get("/api/v1/sonda").header(ORIGIN, ORIGEN_PERMITIDO))
        .andExpect(status().isOk())
        .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_PERMITIDO));
  }

  @Test
  void rechazaElPreflightDeUnOrigenNoPermitido() throws Exception {
    mvc.perform(
            options("/api/v1/sonda")
                .header(ORIGIN, ORIGEN_AJENO)
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void rechazaUnMetodoQueNoEstaPermitido() throws Exception {
    mvc.perform(
            options("/api/v1/sonda")
                .header(ORIGIN, ORIGEN_PERMITIDO)
                .header(ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
        .andExpect(status().isForbidden());
  }

  @Test
  void noAbreCorsFueraDeApi() throws Exception {
    mvc.perform(
            options("/fuera")
                .header(ORIGIN, ORIGEN_PERMITIDO)
                .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  static class Sonda {

    @Bean
    SondaController sondaController() {
      return new SondaController();
    }
  }

  @RestController
  static class SondaController {

    @GetMapping("/api/v1/sonda")
    String consultar() {
      return "ok";
    }

    @PutMapping("/api/v1/sonda")
    String modificar() {
      return "ok";
    }

    @GetMapping("/fuera")
    String fuera() {
      return "ok";
    }
  }
}
