package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface LiquidacionJpaRepository extends JpaRepository<LiquidacionEntity, Long> {

  Optional<LiquidacionEntity> findByIdLoteAndEstado(UUID idLote, EstadoLiquidacion estado);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM LiquidacionEntity l WHERE l.id = :id")
  Optional<LiquidacionEntity> findByIdConBloqueo(long id);
}
