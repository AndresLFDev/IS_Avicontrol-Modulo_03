package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.time.Instant;

/** Estado de la sincronización por fuente (RT-09). */
public record EstadoSincronizacionDto(EstadoFuente modulo1, EstadoFuente modulo2) {

  /**
   * @param ultimaSincronizacionExitosa {@code null} si nunca se ha recibido un evento exitoso
   * @param desactualizada {@code true} si no hay eventos exitosos dentro del umbral configurado
   */
  public record EstadoFuente(Instant ultimaSincronizacionExitosa, boolean desactualizada) {}
}
