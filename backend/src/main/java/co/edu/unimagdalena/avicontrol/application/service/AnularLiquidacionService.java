package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaAnuladaException;
import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.AnularLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.AnulacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * M3-CU04. Solo opera sobre datos de M3 (FR-009) y cambia únicamente el estado (FR-006). El bloqueo
 * de la fila hace que una anulación concurrente espere y encuentre el registro de la primera (caso
 * borde "Concurrencia").
 */
@Service
public class AnularLiquidacionService implements AnularLiquidacionUseCase {

  private final LiquidacionRepository liquidaciones;
  private final Clock clock;

  public AnularLiquidacionService(LiquidacionRepository liquidaciones, Clock clock) {
    this.liquidaciones = liquidaciones;
    this.clock = clock;
  }

  @Override
  @Transactional // FR-008: estado y registro se guardan juntos o no se guarda ninguno
  public AnulacionDto anularLiquidacion(long idLiquidacion, String motivo, String usuario) {
    var liquidacion =
        liquidaciones
            .bloquearParaAnular(idLiquidacion)
            .orElseThrow(() -> new LiquidacionNoEncontradaException(idLiquidacion));
    if (liquidacion.estado() == EstadoLiquidacion.ANULADA) {
      throw new LiquidacionYaAnuladaException(idLiquidacion, liquidacion.registroAnulacion());
    }
    var registro = liquidaciones.anular(idLiquidacion, motivo, usuario, clock.instant());
    return new AnulacionDto(idLiquidacion, EstadoLiquidacion.ANULADA.name(), registro);
  }
}
