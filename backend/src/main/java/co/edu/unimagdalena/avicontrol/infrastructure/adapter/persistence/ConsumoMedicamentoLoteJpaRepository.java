package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ConsumoMedicamentoLoteJpaRepository
    extends JpaRepository<ConsumoMedicamentoLoteEntity, UUID> {}
