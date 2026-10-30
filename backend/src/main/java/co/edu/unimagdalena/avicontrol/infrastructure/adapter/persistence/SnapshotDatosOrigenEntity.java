package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla {@code snapshot_datos_origen}: una fila por fuente usada (CU03.FR-009). */
@Entity
@Table(name = "snapshot_datos_origen")
class SnapshotDatosOrigenEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_liquidacion")
  LiquidacionEntity liquidacion;

  @Column(name = "id_lote", nullable = false)
  UUID idLote;

  @Enumerated(EnumType.STRING)
  @Column(name = "fuente", nullable = false)
  FuenteSincronizacion fuente;

  @Column(name = "fecha_hora_sync", nullable = false)
  Instant fechaHoraSync;

  @Column(name = "poblacion_inicial")
  Integer poblacionInicial;

  @Column(name = "poblacion_actual")
  Integer poblacionActual;

  @Column(name = "costo_total_lote_cop")
  Long costoTotalLoteCop;
}
