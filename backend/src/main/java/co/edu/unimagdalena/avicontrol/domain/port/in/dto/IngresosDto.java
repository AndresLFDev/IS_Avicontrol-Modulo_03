package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.math.BigDecimal;

/** Sección de ingresos del desglose; en siniestro total el promedio y el precio son null. */
public record IngresosDto(
    int pollosVendidos,
    BigDecimal pesoTotalKg,
    BigDecimal pesoPromedioKg,
    BigDecimal precioKgCop,
    long ventaBrutaCop) {}
