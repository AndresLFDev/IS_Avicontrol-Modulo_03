package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla {@code aviso_utilizacion_resultado}: outbox del aviso de utilización para M2. */
@Entity
@Table(name = "aviso_utilizacion_resultado")
class AvisoUtilizacionResultadoEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_aviso")
  Long id;

  @Column(name = "id_resultado", nullable = false, unique = true)
  UUID idResultado;

  @Column(name = "id_liquidacion", nullable = false)
  long idLiquidacion;

  @Column(name = "fecha_hora_emision", nullable = false)
  Instant fechaHoraEmision;

  @Column(name = "estado_entrega", nullable = false)
  String estadoEntrega;

  @Column(name = "intentos", nullable = false)
  int intentos;
}
