package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.EstadoSincronizacionDto;

/** RT-09 — Última sincronización exitosa con Módulo 1 y Módulo 2, y si está desactualizada. */
public interface ConsultarEstadoSincronizacionUseCase {

  EstadoSincronizacionDto consultar();
}
