package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.math.BigDecimal;

/** Fila del desglose con la referencia a su fuente sincronizada (trazabilidad, CU05.SC-003). */
public record PartidaDesgloseDto(
    String concepto,
    BigDecimal cantidad,
    String unidadMedida,
    BigDecimal precioUnitarioCop,
    long subtotalCop,
    String fuenteOrigen,
    String referenciaOrigen) {}
