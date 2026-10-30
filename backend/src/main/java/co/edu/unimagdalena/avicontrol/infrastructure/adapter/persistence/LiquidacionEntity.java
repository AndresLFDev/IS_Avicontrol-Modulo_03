package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tabla {@code liquidacion} (V3). Las partidas y el snapshot se guardan en cascada. */
@Entity
@Table(name = "liquidacion")
class LiquidacionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_liquidacion")
  Long id;

  @Column(name = "id_lote", nullable = false)
  UUID idLote;

  @Column(name = "id_galpon", nullable = false)
  UUID idGalpon;

  @Column(name = "id_resultado_sacrificio")
  UUID idResultadoSacrificio;

  @Column(name = "pollos_vendidos", nullable = false)
  int pollosVendidos;

  @Column(name = "peso_total_kg", nullable = false)
  BigDecimal pesoTotalKg;

  @Column(name = "peso_promedio_kg")
  BigDecimal pesoPromedioKg;

  @Column(name = "precio_kg_cop")
  BigDecimal precioKgCop;

  @Column(name = "venta_bruta_cop", nullable = false)
  long ventaBrutaCop;

  @Column(name = "mortalidad_aves", nullable = false)
  int mortalidadAves;

  @Column(name = "porcentaje_mortalidad", nullable = false)
  BigDecimal porcentajeMortalidad;

  @Column(name = "costos_operativos_cop", nullable = false)
  long costosOperativosCop;

  @Column(name = "utilidad_neta_cop", nullable = false)
  long utilidadNetaCop;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false)
  EstadoLiquidacion estado;

  @Column(name = "fecha_hora_generacion", nullable = false)
  Instant fechaHoraGeneracion;

  @Column(name = "usuario_responsable", nullable = false)
  String usuarioResponsable;

  /** Bloqueo optimista para las anulaciones concurrentes (CU04). */
  @Version
  @Column(name = "version", nullable = false)
  long version;

  @OneToMany(mappedBy = "liquidacion", cascade = CascadeType.ALL)
  List<PartidaCostoEntity> partidas = new ArrayList<>();

  @OneToMany(mappedBy = "liquidacion", cascade = CascadeType.ALL)
  List<SnapshotDatosOrigenEntity> snapshots = new ArrayList<>();

  @OneToOne(mappedBy = "liquidacion")
  RegistroAnulacionEntity registroAnulacion;
}
