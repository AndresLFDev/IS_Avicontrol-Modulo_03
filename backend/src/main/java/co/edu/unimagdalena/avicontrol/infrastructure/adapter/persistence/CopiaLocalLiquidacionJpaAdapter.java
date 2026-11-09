package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.LoteLiquidable;
import co.edu.unimagdalena.avicontrol.domain.model.PartidaAlimento;
import co.edu.unimagdalena.avicontrol.domain.model.ResultadoSacrificio;
import co.edu.unimagdalena.avicontrol.domain.model.TramoMedicamento;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalLiquidacionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Lee la copia local de M1 y M2 (tablas de V1 y V2) para la Liquidación. */
@Repository
public class CopiaLocalLiquidacionJpaAdapter implements CopiaLocalLiquidacionRepository {

  private static final String PENDIENTE_PRECIO = "PENDIENTE_PRECIO";

  private final LoteJpaRepository lotes;
  private final AlertaVaciadoSanitarioJpaRepository alertas;
  private final ResultadoFinalSacrificioJpaRepository resultados;
  private final PartidaAlimentoLoteJpaRepository partidasAlimento;
  private final TramoRecepcionConsumoJpaRepository tramos;

  public CopiaLocalLiquidacionJpaAdapter(
      LoteJpaRepository lotes,
      AlertaVaciadoSanitarioJpaRepository alertas,
      ResultadoFinalSacrificioJpaRepository resultados,
      PartidaAlimentoLoteJpaRepository partidasAlimento,
      TramoRecepcionConsumoJpaRepository tramos) {
    this.lotes = lotes;
    this.alertas = alertas;
    this.resultados = resultados;
    this.partidasAlimento = partidasAlimento;
    this.tramos = tramos;
  }

  @Override
  public Optional<LoteLiquidable> buscarLote(UUID idLote) {
    return lotes
        .findConGalpon(idLote)
        .map(
            l ->
                new LoteLiquidable(
                    l.idLote,
                    l.nombre,
                    l.galpon.idGalpon,
                    l.galpon.nombre,
                    l.poblacionInicial,
                    l.poblacionActual,
                    l.costoTotalCop,
                    l.fechaHoraSync));
  }

  @Override
  public boolean tieneAlertaVaciado(UUID idLote) {
    return alertas.existsByIdLote(idLote);
  }

  @Override
  public Optional<ResultadoSacrificio> buscarResultadoSacrificio(UUID idLote) {
    return resultados
        .findByIdLote(idLote)
        .map(
            r ->
                new ResultadoSacrificio(
                    r.idResultado, r.cantidadFinalPollos, r.pesoTotalKg, r.fechaHoraSync));
  }

  @Override
  public List<PartidaAlimento> buscarPartidasAlimento(UUID idLote) {
    return partidasAlimento.findByIdLoteOrderByIdPartidaOrigen(idLote).stream()
        .map(
            p ->
                new PartidaAlimento(
                    p.idPartidaOrigen,
                    p.tipoAlimento,
                    p.cantidadKg,
                    p.precioUnitarioKgCop,
                    pendiente(p.estadoValorizacion, p.precioUnitarioKgCop),
                    p.referenciaOrigen,
                    p.fechaHoraSync))
        .toList();
  }

  @Override
  public List<TramoMedicamento> buscarTramosMedicamento(UUID idLote) {
    return tramos.findDelLote(idLote).stream()
        .map(
            t ->
                new TramoMedicamento(
                    t.consumo.idConsumoOrigen,
                    t.consumo.medicamento,
                    t.referenciaRecepcion,
                    t.cantidadUnidadBase,
                    t.consumo.unidadBase,
                    t.precioUnitarioBaseCop,
                    pendiente(t.estadoValorizacion, t.precioUnitarioBaseCop),
                    t.consumo.fechaHoraSync))
        .toList();
  }

  /** Sin precio también cuenta como pendiente, aunque el estado diga VALORIZADA. */
  private static boolean pendiente(String estadoValorizacion, BigDecimal precio) {
    return PENDIENTE_PRECIO.equals(estadoValorizacion) || precio == null;
  }
}
