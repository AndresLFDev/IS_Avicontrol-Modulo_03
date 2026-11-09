package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla {@code galpon}: copia local del galpón de M1. */
@Entity
@Table(name = "galpon")
class GalponEntity {

  @Id
  @Column(name = "id_galpon")
  UUID idGalpon;

  @Column(name = "nombre", nullable = false)
  String nombre;

  @Column(name = "aforo_maximo", nullable = false)
  int aforoMaximo;

  @Column(name = "estado", nullable = false)
  String estado;

  @Column(name = "fecha_hora_sync", nullable = false)
  Instant fechaHoraSync;
}
