package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PartidaAlimentoLoteJpaRepository extends JpaRepository<PartidaAlimentoLoteEntity, UUID> {

  List<PartidaAlimentoLoteEntity> findByIdLoteOrderByIdPartidaOrigen(UUID idLote);
}
