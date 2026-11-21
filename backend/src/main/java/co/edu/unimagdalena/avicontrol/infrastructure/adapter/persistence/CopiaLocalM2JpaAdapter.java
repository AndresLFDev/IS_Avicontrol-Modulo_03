package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteConsultableM2;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM2Repository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escritura de la copia local de M2: {@code resultado_final_sacrificio}, {@code
 * partida_alimento_lote}, {@code consumo_medicamento_lote} y {@code tramo_recepcion_consumo}.
 *
 * <p>Cada escritura compara con lo que ya hay y, si es igual, no toca la fila. Así {@code
 * fecha_hora_sync} solo cambia cuando cambian los datos: la huella de la Liquidación la incluye, y
 * si se renovara en cada consulta una vista previa se invalidaría sola a los pocos minutos.
 */
@Repository
public class CopiaLocalM2JpaAdapter implements CopiaLocalM2Repository {

  static final int DECIMALES_CANTIDAD = 4;
  static final int DECIMALES_PRECIO = 4;
  static final int DECIMALES_IMPUESTO = 2;
  static final int DECIMALES_PESO = 2;

  private static final String NO_UTILIZADO = "NO_UTILIZADO";
  private static final String UTILIZADO = "UTILIZADO";
  private static final String VALORIZADA = "VALORIZADA";
  private static final String PENDIENTE_PRECIO = "PENDIENTE_PRECIO";

  private final LoteJpaRepository lotes;
  private final ResultadoFinalSacrificioJpaRepository resultados;
  private final PartidaAlimentoLoteJpaRepository partidasAlimento;
  private final ConsumoMedicamentoLoteJpaRepository consumos;
  private final TramoRecepcionConsumoJpaRepository tramos;

  public CopiaLocalM2JpaAdapter(
      LoteJpaRepository lotes,
      ResultadoFinalSacrificioJpaRepository resultados,
      PartidaAlimentoLoteJpaRepository partidasAlimento,
      ConsumoMedicamentoLoteJpaRepository consumos,
      TramoRecepcionConsumoJpaRepository tramos) {
    this.lotes = lotes;
    this.resultados = resultados;
    this.partidasAlimento = partidasAlimento;
    this.consumos = consumos;
    this.tramos = tramos;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LoteConsultableM2> lotesAConsultar() {
    return lotes.findConsultablesM2();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ResultadoLocal> resultadoDelLote(UUID idLote) {
    return resultados
        .findByIdLote(idLote)
        .map(r -> new ResultadoLocal(r.idResultado, UTILIZADO.equals(r.estadoUtilizacion)));
  }

  @Override
  @Transactional
  public void guardarResultado(
      UUID idResultado,
      UUID idLote,
      UUID idGalpon,
      int cantidadFinalPollos,
      BigDecimal pesoTotalKg,
      Instant fechaRegistro,
      Instant ahora) {
    BigDecimal peso = escala(pesoTotalKg, DECIMALES_PESO);
    Instant registro = fechaRegistro.truncatedTo(ChronoUnit.MICROS);
    var existente = resultados.findByIdLote(idLote);
    boolean nuevo = existente.isEmpty();
    var resultado = existente.orElseGet(ResultadoFinalSacrificioEntity::new);
    if (!nuevo
        && resultado.idGalpon.equals(idGalpon)
        && resultado.cantidadFinalPollos == cantidadFinalPollos
        && igual(resultado.pesoTotalKg, peso)
        && resultado.fechaRegistro.equals(registro)) {
      return;
    }
    if (nuevo) {
      resultado.idResultado = idResultado;
      resultado.estadoUtilizacion = NO_UTILIZADO;
    }
    resultado.idLote = idLote;
    resultado.idGalpon = idGalpon;
    resultado.cantidadFinalPollos = cantidadFinalPollos;
    resultado.pesoTotalKg = peso;
    resultado.fechaRegistro = registro;
    resultado.fechaHoraSync = ahora;
    resultados.saveAndFlush(resultado);
  }

  @Override
  @Transactional
  public void guardarPartidaAlimento(
      UUID idPartida,
      UUID idLote,
      UUID idGalpon,
      String tipoAlimento,
      BigDecimal cantidadKg,
      BigDecimal precioUnitarioKgCop,
      BigDecimal valorImpuestoCop,
      String referenciaOrigen,
      Instant ahora) {
    BigDecimal cantidad = escala(cantidadKg, DECIMALES_CANTIDAD);
    BigDecimal precio = escala(precioUnitarioKgCop, DECIMALES_PRECIO);
    BigDecimal impuesto = escala(valorImpuestoCop, DECIMALES_IMPUESTO);
    String estado = precio == null ? PENDIENTE_PRECIO : VALORIZADA;
    var existente = partidasAlimento.findById(idPartida);
    var partida = existente.orElseGet(PartidaAlimentoLoteEntity::new);
    if (existente.isPresent()
        && partida.idLote.equals(idLote)
        && partida.idGalpon.equals(idGalpon)
        && partida.tipoAlimento.equals(tipoAlimento)
        && igual(partida.cantidadKg, cantidad)
        && igual(partida.precioUnitarioKgCop, precio)
        && igual(partida.valorImpuestoCop, impuesto)
        && partida.estadoValorizacion.equals(estado)
        && Objects.equals(partida.referenciaOrigen, referenciaOrigen)) {
      return;
    }
    partida.idPartidaOrigen = idPartida;
    partida.idLote = idLote;
    partida.idGalpon = idGalpon;
    partida.tipoAlimento = tipoAlimento;
    partida.cantidadKg = cantidad;
    partida.precioUnitarioKgCop = precio;
    partida.valorImpuestoCop = impuesto;
    partida.estadoValorizacion = estado;
    partida.referenciaOrigen = referenciaOrigen;
    partida.fechaHoraSync = ahora;
    partidasAlimento.saveAndFlush(partida);
  }

  @Override
  @Transactional
  public void guardarConsumoMedicamento(
      UUID idConsumo,
      UUID idLote,
      UUID idGalpon,
      String medicamento,
      LocalDate fechaConsumo,
      BigDecimal cantidadUnidadBase,
      String unidadBase,
      Instant ahora) {
    BigDecimal cantidad = escala(cantidadUnidadBase, DECIMALES_CANTIDAD);
    var existente = consumos.findById(idConsumo);
    var consumo = existente.orElseGet(ConsumoMedicamentoLoteEntity::new);
    if (existente.isPresent()
        && consumo.idLote.equals(idLote)
        && consumo.idGalpon.equals(idGalpon)
        && consumo.medicamento.equals(medicamento)
        && consumo.fechaConsumo.equals(fechaConsumo)
        && igual(consumo.cantidadUnidadBase, cantidad)
        && consumo.unidadBase.equals(unidadBase)) {
      return;
    }
    consumo.idConsumoOrigen = idConsumo;
    consumo.idLote = idLote;
    consumo.idGalpon = idGalpon;
    consumo.medicamento = medicamento;
    consumo.fechaConsumo = fechaConsumo;
    consumo.cantidadUnidadBase = cantidad;
    consumo.unidadBase = unidadBase;
    consumo.fechaHoraSync = ahora;
    consumos.saveAndFlush(consumo);
  }

  @Override
  @Transactional
  public void guardarTramoMedicamento(
      UUID idConsumo,
      String referenciaRecepcion,
      BigDecimal cantidadUnidadBase,
      BigDecimal precioUnitarioBaseCop,
      BigDecimal valorImpuestoCop) {
    BigDecimal cantidad = escala(cantidadUnidadBase, DECIMALES_CANTIDAD);
    BigDecimal precio = escala(precioUnitarioBaseCop, DECIMALES_PRECIO);
    BigDecimal impuesto = escala(valorImpuestoCop, DECIMALES_IMPUESTO);
    String estado = precio == null ? PENDIENTE_PRECIO : VALORIZADA;
    var existente = tramos.findDelConsumo(idConsumo, referenciaRecepcion);
    var tramo = existente.orElseGet(TramoRecepcionConsumoEntity::new);
    if (existente.isPresent()
        && igual(tramo.cantidadUnidadBase, cantidad)
        && igual(tramo.precioUnitarioBaseCop, precio)
        && igual(tramo.valorImpuestoCop, impuesto)
        && tramo.estadoValorizacion.equals(estado)) {
      return;
    }
    if (existente.isEmpty()) {
      // findById (no getReferenceById): el consumo se escribió en esta misma transacción
      tramo.consumo =
          consumos
              .findById(idConsumo)
              .orElseThrow(() -> new IllegalStateException("No existe el consumo " + idConsumo));
      tramo.referenciaRecepcion = referenciaRecepcion;
    }
    tramo.cantidadUnidadBase = cantidad;
    tramo.precioUnitarioBaseCop = precio;
    tramo.valorImpuestoCop = impuesto;
    tramo.estadoValorizacion = estado;
    tramos.saveAndFlush(tramo);
  }

  /** Lleva el valor a la escala de la columna; así lo leído y lo recibido se comparan igual. */
  private static BigDecimal escala(BigDecimal valor, int decimales) {
    return valor == null ? null : valor.setScale(decimales, RoundingMode.HALF_UP);
  }

  private static boolean igual(BigDecimal a, BigDecimal b) {
    return a == null ? b == null : b != null && a.compareTo(b) == 0;
  }
}
