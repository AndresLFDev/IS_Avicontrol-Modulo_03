package co.edu.unimagdalena.avicontrol.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita {@code @Scheduled}, usado para publicar los avisos de utilización pendientes. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}
