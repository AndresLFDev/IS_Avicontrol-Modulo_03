package co.edu.unimagdalena.avicontrol.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de la documentación interactiva publicada en {@code /swagger-ui.html}. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

  @Bean
  public OpenAPI avicontrolOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("AviControl Módulo 3 — Liquidación de Lote")
                .description(
                    "API de liquidación de lotes y análisis de rentabilidad. Los errores siguen"
                        + " RFC 7807 (application/problem+json).")
                .version("v1"));
  }
}
