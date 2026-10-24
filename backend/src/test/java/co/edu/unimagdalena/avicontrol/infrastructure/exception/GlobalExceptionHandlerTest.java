package co.edu.unimagdalena.avicontrol.infrastructure.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private static final Clock RELOJ =
      Clock.fixed(Instant.parse("2026-10-05T19:30:00Z"), ZoneOffset.UTC);

  private MockMvc mvc;

  @BeforeEach
  void configurar() {
    var validador = new LocalValidatorFactoryBean();
    validador.afterPropertiesSet();
    mvc =
        MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
            .setControllerAdvice(new GlobalExceptionHandler(RELOJ))
            .setValidator(validador)
            .build();
  }

  @Test
  void bodyInvalidoResponde400ConErroresPorCampo() throws Exception {
    mvc.perform(
            post("/prueba")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\": \"\", \"precioKgCop\": 0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/validacion"))
        .andExpect(jsonPath("$.title").value("Error de validación"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.instance").value("/prueba"))
        .andExpect(jsonPath("$.timestamp").value("2026-10-05T19:30:00Z"))
        .andExpect(jsonPath("$.errores[*].campo").value(hasItem("usuario")))
        .andExpect(jsonPath("$.errores[*].campo").value(hasItem("precioKgCop")));
  }

  @Test
  void jsonIlegibleResponde400() throws Exception {
    mvc.perform(post("/prueba").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/validacion"));
  }

  @Test
  void parametroDeTipoIncorrectoResponde400() throws Exception {
    mvc.perform(get("/prueba/pagina").param("page", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/validacion"))
        .andExpect(jsonPath("$.errores[0].campo").value("page"));
  }

  @Test
  void parametroFueraDeRangoResponde400() throws Exception {
    mvc.perform(get("/prueba/pagina").param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("page"));
  }

  @Test
  void parametroObligatorioAusenteResponde400() throws Exception {
    mvc.perform(get("/prueba/pagina"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("page"));
  }

  @Test
  void errorNoControladoResponde500SinExponerElMensaje() throws Exception {
    mvc.perform(get("/prueba/falla"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/error-interno"))
        .andExpect(jsonPath("$.detail").value(not(containsString("secreto"))))
        .andExpect(jsonPath("$.timestamp").value("2026-10-05T19:30:00Z"));
  }

  @RestController
  @RequestMapping("/prueba")
  static class ControladorDePrueba {

    record Solicitud(
        @NotBlank(message = "El usuario responsable es obligatorio.") String usuario,
        @Positive(message = "El precio por kg debe ser mayor que 0.") BigDecimal precioKgCop) {}

    @PostMapping
    void crear(@Valid @RequestBody Solicitud solicitud) {}

    @GetMapping("/pagina")
    int pagina(@RequestParam @Min(0) int page) {
      return page;
    }

    @GetMapping("/falla")
    void falla() {
      throw new IllegalStateException("detalle secreto");
    }
  }
}
