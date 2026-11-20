package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.util.UUID;

/**
 * Galpón tal como lo informa Módulo 1. En la lista {@code loteActivo} es siempre nulo; en el
 * detalle viene solo si el galpón tiene un lote activo (M1 omite el campo, nunca envía {@code
 * null}).
 */
public record GalponM1(UUID id, String nombre, Long capacidad, String estado, LoteM1 loteActivo) {}
