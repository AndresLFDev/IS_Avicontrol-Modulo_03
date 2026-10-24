package co.edu.unimagdalena.avicontrol;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL y Kafka reales para las pruebas de integración (*IT). {@code @ServiceConnection}
 * configura la URL de la base de datos y el {@code bootstrap-servers} automáticamente. Las imágenes
 * son las mismas de {@code compose.yaml}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
  }

  @Bean
  @ServiceConnection
  KafkaContainer kafka() {
    return new KafkaContainer(DockerImageName.parse("apache/kafka:4.3.2"));
  }
}
