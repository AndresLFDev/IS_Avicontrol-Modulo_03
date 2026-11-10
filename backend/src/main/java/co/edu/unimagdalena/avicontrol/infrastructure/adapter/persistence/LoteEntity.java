package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Tabla {@code lote}: copia local del lote de M1. */
@Entity
@Table(name = "lote")
class LoteEntity {

  @Id
  @Column(name = "id_lote")
  UUID idLote;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_galpon")
  GalponEntity galpon;

  @Column(name = "nombre", nullable = false)
  String nombre;

  @Column(name = "fecha_ingreso", nullable = false)
  LocalDate fechaIngreso;

  @Column(name = "poblacion_inicial", nullable = false)
  int poblacionInicial;

  @Column(name = "poblacion_actual", nullable = false)
  int poblacionActual;

  @Column(name = "costo_total_cop", nullable = false)
  long costoTotalCop;

  @Column(name = "estado", nullable = false)
  String estado;

  @Column(name = "fecha_hora_sync", nullable = false)
  Instant fechaHoraSync;
}
