package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Tabla {@code registro_anulacion}. Se escribe en CU04; aquí solo se lee. */
@Entity
@Table(name = "registro_anulacion")
class RegistroAnulacionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_anulacion")
  Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_liquidacion")
  LiquidacionEntity liquidacion;

  @Column(name = "motivo", nullable = false)
  String motivo;

  @Column(name = "fecha_hora_anulacion", nullable = false)
  Instant fechaHoraAnulacion;

  @Column(name = "usuario_responsable", nullable = false)
  String usuarioResponsable;
}
