package co.edu.unimagdalena.avicontrol.domain.model;

import java.math.BigDecimal;

/** Partida valorizada que se congela en {@code partida_costo_lote} al generar la Liquidación. */
public record PartidaCosto(
    CategoriaCosto categoria,
    String concepto,
    BigDecimal cantidad,
    String unidadMedida,
    BigDecimal precioUnitarioCop,
    long subtotalCop,
    FuenteSincronizacion fuenteOrigen,
    String referenciaOrigen) {}
