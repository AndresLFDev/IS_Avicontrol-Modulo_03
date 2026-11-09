package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AlertaVaciadoSanitarioJpaRepository
    extends JpaRepository<AlertaVaciadoSanitarioEntity, UUID> {

  boolean existsByIdLote(UUID idLote);
}
