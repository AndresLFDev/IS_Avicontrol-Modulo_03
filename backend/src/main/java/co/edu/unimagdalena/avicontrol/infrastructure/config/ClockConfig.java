package co.edu.unimagdalena.avicontrol.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único de la aplicación. Los servicios reciben {@link Clock} por inyección en lugar de
 * llamar a {@code Instant.now()}, para que las fechas de auditoría sean deterministas en pruebas.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
