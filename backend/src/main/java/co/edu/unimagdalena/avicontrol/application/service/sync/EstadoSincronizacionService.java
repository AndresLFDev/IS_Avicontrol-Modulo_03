package co.edu.unimagdalena.avicontrol.application.service.sync;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarEstadoSincronizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto.EstadoFuente;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RT-09 (plan 001, T017). Una fuente está desactualizada si su último evento exitoso es más antiguo
 * que {@code sync.umbral-inactividad} o si nunca ha llegado ninguno.
 */
@Service
public class EstadoSincronizacionService implements ConsultarEstadoSincronizacionUseCase {

  private final RegistroSincronizacionRepository registros;
  private final Clock clock;
  private final Duration umbral;

  public EstadoSincronizacionService(
      RegistroSincronizacionRepository registros,
      Clock clock,
      @Value("${sync.umbral-inactividad}") Duration umbral) {
    this.registros = registros;
    this.clock = clock;
    this.umbral = umbral;
  }

  @Override
  @Transactional(readOnly = true)
  public EstadoSincronizacionDto consultar() {
    return new EstadoSincronizacionDto(
        estado(FuenteSincronizacion.MODULO_1), estado(FuenteSincronizacion.MODULO_2));
  }

  private EstadoFuente estado(FuenteSincronizacion fuente) {
    Instant ultima = registros.ultimaSincronizacionExitosa(fuente).orElse(null);
    boolean desactualizada = ultima == null || ultima.isBefore(Instant.now(clock).minus(umbral));
    return new EstadoFuente(ultima, desactualizada);
  }
}
