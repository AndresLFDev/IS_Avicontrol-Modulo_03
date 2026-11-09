package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Tabla {@code consumo_medicamento_lote}: consumos de medicamento que envía el Módulo 2. */
@Entity
@Table(name = "consumo_medicamento_lote")
class ConsumoMedicamentoLoteEntity {

  @Id
  @Column(name = "id_consumo_origen")
  UUID idConsumoOrigen;

  @Column(name = "id_lote", nullable = false)
  UUID idLote;

  @Column(name = "id_galpon", nullable = false)
  UUID idGalpon;

  @Column(name = "medicamento", nullable = false)
  String medicamento;

  @Column(name = "fecha_consumo", nullable = false)
  LocalDate fechaConsumo;

  @Column(name = "cantidad_unidad_base", nullable = false)
  BigDecimal cantidadUnidadBase;

  @Column(name = "unidad_base", nullable = false)
  String unidadBase;

  @Column(name = "fecha_hora_sync", nullable = false)
  Instant fechaHoraSync;
}
