package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RegistroSincronizacionJpaRepository
    extends JpaRepository<RegistroSincronizacionEntity, Long> {

  /** Fines de las sincronizaciones exitosas de la fuente, de la más reciente a la más antigua. */
  @Query(
      """
      SELECT r.fechaHoraFin FROM RegistroSincronizacionEntity r
       WHERE r.fuente = :fuente AND r.resultado = 'EXITOSA' AND r.fechaHoraFin IS NOT NULL
       ORDER BY r.fechaHoraFin DESC
      """)
  List<Instant> fechasFinExitosas(FuenteSincronizacion fuente, Limit limite);
}
