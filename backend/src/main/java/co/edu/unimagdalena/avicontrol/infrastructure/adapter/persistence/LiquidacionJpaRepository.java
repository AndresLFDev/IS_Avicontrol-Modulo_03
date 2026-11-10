package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialItemDto;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface LiquidacionJpaRepository extends JpaRepository<LiquidacionEntity, Long> {

  /**
   * Base del historial (M3-CU06.FR-003). El galpón se toma de la Liquidación, no del lote, para que
   * las de lotes ya desvinculados sigan apareciendo bajo el galpón histórico.
   */
  String HISTORIAL_FILTRADO =
      "FROM LiquidacionEntity liq"
          + " JOIN LoteEntity l ON l.idLote = liq.idLote"
          + " JOIN GalponEntity g ON g.idGalpon = liq.idGalpon"
          + " WHERE (cast(:search as String) IS NULL"
          + " OR l.nombre ilike cast(:patron as String) escape '!'"
          + " OR g.nombre ilike cast(:patron as String) escape '!'"
          + " OR cast(liq.idLote as String) = lower(cast(:search as String))"
          + " OR cast(liq.idGalpon as String) = lower(cast(:search as String)))"
          + " AND (cast(:desde as Instant) IS NULL OR liq.fechaHoraGeneracion >= :desde)"
          + " AND (cast(:hasta as Instant) IS NULL OR liq.fechaHoraGeneracion < :hasta)";

  Optional<LiquidacionEntity> findByIdLoteAndEstado(UUID idLote, EstadoLiquidacion estado);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM LiquidacionEntity l WHERE l.id = :id")
  Optional<LiquidacionEntity> findByIdConBloqueo(long id);

  /** Partidas congeladas de la Liquidación, en el orden en que se guardaron. */
  @Query("SELECT p FROM PartidaCostoEntity p WHERE p.liquidacion.id = :idLiquidacion ORDER BY p.id")
  List<PartidaCostoEntity> findPartidas(long idLiquidacion);

  @Query(
      value =
          "SELECT new co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialItemDto("
              + "liq.id, liq.fechaHoraGeneracion, liq.idGalpon, g.nombre, liq.idLote, l.nombre,"
              + " liq.ventaBrutaCop, liq.porcentajeMortalidad, liq.costosOperativosCop,"
              + " liq.utilidadNetaCop, cast(liq.estado as String)) "
              + HISTORIAL_FILTRADO
              + " ORDER BY liq.fechaHoraGeneracion DESC, liq.id DESC",
      countQuery = "SELECT COUNT(liq) " + HISTORIAL_FILTRADO)
  Page<HistorialItemDto> buscarHistorial(
      String search, String patron, Instant desde, Instant hasta, Pageable paginacion);
}
