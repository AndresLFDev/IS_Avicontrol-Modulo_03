package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Tabla {@code tramo_recepcion_consumo}: tramos por recepción de cada consumo de medicamento. */
@Entity
@Table(name = "tramo_recepcion_consumo")
class TramoRecepcionConsumoEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_tramo")
  Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_consumo_origen")
  ConsumoMedicamentoLoteEntity consumo;

  @Column(name = "referencia_recepcion", nullable = false)
  String referenciaRecepcion;

  @Column(name = "cantidad_unidad_base", nullable = false)
  BigDecimal cantidadUnidadBase;

  @Column(name = "precio_unitario_base_cop")
  BigDecimal precioUnitarioBaseCop;

  @Column(name = "valor_impuesto_cop")
  BigDecimal valorImpuestoCop;

  @Column(name = "estado_valorizacion", nullable = false)
  String estadoValorizacion;
}
