package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LoteJpaRepository extends JpaRepository<LoteEntity, UUID> {

  /** El lote con su galpón en una sola consulta. */
  @Query("SELECT l FROM LoteEntity l JOIN FETCH l.galpon WHERE l.idLote = :idLote")
  Optional<LoteEntity> findConGalpon(UUID idLote);
}
