package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Tabla {@code partida_alimento_lote}: partidas de alimento que envía el Módulo 2. */
@Entity
@Table(name = "partida_alimento_lote")
class PartidaAlimentoLoteEntity {

  @Id
  @Column(name = "id_partida_origen")
  UUID idPartidaOrigen;

  @Column(name = "id_lote", nullable = false)
  UUID idLote;

  @Column(name = "id_galpon", nullable = false)
  UUID idGalpon;

  @Column(name = "tipo_alimento", nullable = false)
  String tipoAlimento;

  @Column(name = "cantidad_kg", nullable = false)
  BigDecimal cantidadKg;

  @Column(name = "precio_unitario_kg_cop")
  BigDecimal precioUnitarioKgCop;

  @Column(name = "valor_impuesto_cop")
  BigDecimal valorImpuestoCop;

  @Column(name = "estado_valorizacion", nullable = false)
  String estadoValorizacion;

  @Column(name = "referencia_origen")
  String referenciaOrigen;

  @Column(name = "fecha_hora_sync", nullable = false)
  Instant fechaHoraSync;
}
