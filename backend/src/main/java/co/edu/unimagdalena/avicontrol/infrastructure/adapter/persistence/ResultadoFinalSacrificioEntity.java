package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Tabla {@code resultado_final_sacrificio}: copia local del resultado que envía el Módulo 2. */
@Entity
@Table(name = "resultado_final_sacrificio")
class ResultadoFinalSacrificioEntity {

  @Id
  @Column(name = "id_resultado")
  UUID idResultado;

  @Column(name = "id_lote", nullable = false)
  UUID idLote;

  @Column(name = "id_galpon", nullable = false)
  UUID idGalpon;

  @Column(name = "cantidad_final_pollos", nullable = false)
  int cantidadFinalPollos;

  @Column(name = "peso_total_kg", nullable = false)
  BigDecimal pesoTotalKg;

  @Column(name = "fecha_registro", nullable = false)
  Instant fechaRegistro;

  @Column(name = "fecha_hora_sync", nullable = false)
  Instant fechaHoraSync;

  @Column(name = "estado_utilizacion", nullable = false)
  String estadoUtilizacion;
}
