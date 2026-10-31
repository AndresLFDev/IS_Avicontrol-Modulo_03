package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.CategoriaCosto;
import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Tabla {@code partida_costo_lote}: partida congelada al generar la Liquidación. */
@Entity
@Table(name = "partida_costo_lote")
class PartidaCostoEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_partida")
  Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_liquidacion")
  LiquidacionEntity liquidacion;

  @Column(name = "id_lote", nullable = false)
  UUID idLote;

  @Enumerated(EnumType.STRING)
  @Column(name = "categoria", nullable = false)
  CategoriaCosto categoria;

  @Column(name = "concepto", nullable = false)
  String concepto;

  @Column(name = "cantidad", nullable = false)
  BigDecimal cantidad;

  @Column(name = "unidad_medida", nullable = false)
  String unidadMedida;

  @Column(name = "precio_unitario_cop", nullable = false)
  BigDecimal precioUnitarioCop;

  @Column(name = "subtotal_cop", nullable = false)
  long subtotalCop;

  @Enumerated(EnumType.STRING)
  @Column(name = "fuente_origen", nullable = false)
  FuenteSincronizacion fuenteOrigen;

  @Column(name = "referencia_origen")
  String referenciaOrigen;
}
