package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla {@code alerta_vaciado_sanitario}: marca el lote como listo para liquidar. */
@Entity
@Table(name = "alerta_vaciado_sanitario")
class AlertaVaciadoSanitarioEntity {

  @Id
  @Column(name = "id_alerta")
  UUID idAlerta;

  @Column(name = "id_galpon", nullable = false)
  UUID idGalpon;

  @Column(name = "id_lote", nullable = false, unique = true)
  UUID idLote;

  @Column(name = "fecha_hora_evento", nullable = false)
  Instant fechaHoraEvento;

  @Column(name = "id_registro_sincronizacion", nullable = false)
  long idRegistroSincronizacion;
}
