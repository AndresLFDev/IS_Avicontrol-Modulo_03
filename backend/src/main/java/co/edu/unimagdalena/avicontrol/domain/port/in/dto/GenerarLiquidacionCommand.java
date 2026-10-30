package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code precioKgCop} es nulo en siniestro total; {@code huellaDatos} es nulo en la vista previa.
 */
public record GenerarLiquidacionCommand(
    UUID idLote, BigDecimal precioKgCop, String usuario, String huellaDatos) {}
