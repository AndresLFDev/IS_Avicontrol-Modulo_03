package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface TramoRecepcionConsumoJpaRepository
    extends JpaRepository<TramoRecepcionConsumoEntity, Long> {

  /** Los tramos de los consumos del lote, con su consumo, en orden estable. */
  @Query(
      """
      SELECT t FROM TramoRecepcionConsumoEntity t JOIN FETCH t.consumo c
       WHERE c.idLote = :idLote
       ORDER BY c.idConsumoOrigen, t.referenciaRecepcion
      """)
  List<TramoRecepcionConsumoEntity> findDelLote(UUID idLote);
}
