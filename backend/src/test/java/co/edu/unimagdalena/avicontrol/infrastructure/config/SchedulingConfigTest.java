package co.edu.unimagdalena.avicontrol.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

/**
 * La configuración habilita {@code @Scheduled}: sin ella, ninguna tarea programada se ejecutaría.
 */
class SchedulingConfigTest {

  @Test
  void habilitaElProcesamientoDeScheduled() {
    try (var contexto = new AnnotationConfigApplicationContext(SchedulingConfig.class)) {
      assertThat(contexto.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class)).hasSize(1);
    }
  }
}
