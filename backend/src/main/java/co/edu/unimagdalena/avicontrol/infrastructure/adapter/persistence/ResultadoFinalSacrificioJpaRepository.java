package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ResultadoFinalSacrificioJpaRepository
    extends JpaRepository<ResultadoFinalSacrificioEntity, UUID> {

  Optional<ResultadoFinalSacrificioEntity> findByIdLote(UUID idLote);
}
