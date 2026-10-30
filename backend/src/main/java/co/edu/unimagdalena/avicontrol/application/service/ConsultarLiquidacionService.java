package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.Liquidacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU03.FR-014: la Liquidación tal como se emitió, sin recalcular ni consultar a M1 o M2. */
@Service
public class ConsultarLiquidacionService implements ConsultarLiquidacionUseCase {

  static final String VER_DESGLOSE = "VER_DESGLOSE";
  static final String ANULAR = "ANULAR";

  private final LiquidacionRepository liquidaciones;

  public ConsultarLiquidacionService(LiquidacionRepository liquidaciones) {
    this.liquidaciones = liquidaciones;
  }

  @Override
  @Transactional(readOnly = true)
  public LiquidacionDto consultarLiquidacion(long idLiquidacion) {
    return liquidaciones
        .buscarPorId(idLiquidacion)
        .map(ConsultarLiquidacionService::aDto)
        .orElseThrow(() -> new LiquidacionNoEncontradaException(idLiquidacion));
  }

  private static LiquidacionDto aDto(Liquidacion l) {
    // CU04.FR-002b: la anulación solo se ofrece sobre una Liquidación ACTIVA
    var acciones =
        l.estado() == EstadoLiquidacion.ACTIVA
            ? List.of(VER_DESGLOSE, ANULAR)
            : List.of(VER_DESGLOSE);
    return new LiquidacionDto(
        l.idLiquidacion(),
        l.estado().name(),
        l.idLote(),
        l.idGalpon(),
        l.nombreLote(),
        l.nombreGalpon(),
        l.siniestroTotal(),
        l.precioKgCop(),
        l.pollosVendidos(),
        l.pesoTotalKg(),
        l.pesoPromedioKg(),
        l.ventaBrutaCop(),
        l.mortalidadAves(),
        l.porcentajeMortalidad(),
        l.subtotales(),
        l.costosOperativosCop(),
        l.utilidadNetaCop(),
        l.fechaHoraGeneracion(),
        l.usuarioResponsable(),
        l.fechaSincronizacionM1(),
        l.fechaSincronizacionM2(),
        l.registroAnulacion(),
        acciones);
  }
}
