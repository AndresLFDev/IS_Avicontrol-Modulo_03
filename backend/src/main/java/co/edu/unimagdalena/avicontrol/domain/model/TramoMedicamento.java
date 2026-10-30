package co.edu.unimagdalena.avicontrol.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Tramo de un consumo de medicamento valorizado con el precio de su recepción (M3-CU10). Un consumo
 * que abarca dos recepciones llega como dos tramos; los precios no se promedian.
 */
public record TramoMedicamento(
    UUID idConsumoOrigen,
    String medicamento,
    String referenciaRecepcion,
    BigDecimal cantidadUnidadBase,
    String unidadBase,
    BigDecimal precioUnitarioBaseCop,
    boolean pendientePrecio,
    Instant fechaHoraSync) {}
