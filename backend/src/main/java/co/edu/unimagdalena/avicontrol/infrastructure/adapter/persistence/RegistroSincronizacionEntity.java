package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla {@code registro_sincronizacion}: bitácora de cada evento recibido de M1 o M2. */
@Entity
@Table(name = "registro_sincronizacion")
class RegistroSincronizacionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "fuente", nullable = false)
  FuenteSincronizacion fuente;

  @Column(name = "topico", nullable = false)
  String topico;

  @Column(name = "id_evento")
  UUID idEvento;

  @Column(name = "fecha_hora_inicio", nullable = false)
  Instant fechaHoraInicio;

  @Column(name = "fecha_hora_fin")
  Instant fechaHoraFin;

  @Column(name = "resultado")
  String resultado;

  @Column(name = "descripcion_error")
  String descripcionError;

  @Column(name = "registros_actualizados", nullable = false)
  int registrosActualizados;
}
