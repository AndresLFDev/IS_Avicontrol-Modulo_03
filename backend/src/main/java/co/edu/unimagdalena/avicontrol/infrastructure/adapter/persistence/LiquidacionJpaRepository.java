package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LiquidacionJpaRepository extends JpaRepository<LiquidacionEntity, Long> {

  Optional<LiquidacionEntity> findByIdLoteAndEstado(UUID idLote, EstadoLiquidacion estado);
}
