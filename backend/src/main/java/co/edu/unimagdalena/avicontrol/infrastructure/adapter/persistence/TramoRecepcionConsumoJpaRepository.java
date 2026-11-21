package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface TramoRecepcionConsumoJpaRepository
    extends JpaRepository<TramoRecepcionConsumoEntity, Long> {

  /** El tramo de un consumo para una recepción (único por la restricción uq_tramo_recepcion). */
  @Query(
      """
      SELECT t FROM TramoRecepcionConsumoEntity t
       WHERE t.consumo.idConsumoOrigen = :idConsumo AND t.referenciaRecepcion = :referencia
      """)
  Optional<TramoRecepcionConsumoEntity> findDelConsumo(UUID idConsumo, String referencia);

  /** Los tramos de los consumos del lote, con su consumo, en orden estable. */
  @Query(
      """
      SELECT t FROM TramoRecepcionConsumoEntity t JOIN FETCH t.consumo c
       WHERE c.idLote = :idLote
       ORDER BY c.idConsumoOrigen, t.referenciaRecepcion
      """)
  List<TramoRecepcionConsumoEntity> findDelLote(UUID idLote);
}
