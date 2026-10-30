package co.edu.unimagdalena.avicontrol.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Permite que el frontend (Vite, otro origen) consuma {@code /api/**} desde el navegador. */
@Configuration(proxyBeanMethods = false)
public class CorsConfig implements WebMvcConfigurer {

  private final String[] origenes;

  public CorsConfig(@Value("${cors.origenes-permitidos}") String[] origenes) {
    this.origenes = origenes;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(origenes)
        .allowedMethods("GET", "POST", "PUT", "DELETE");
  }
}
