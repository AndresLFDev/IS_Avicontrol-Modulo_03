package co.edu.unimagdalena.avicontrol.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Partida de alimento sincronizada desde Módulo 2 (M3-CU09). El precio es nulo si está pendiente.
 */
public record PartidaAlimento(
    UUID idPartidaOrigen,
    String tipoAlimento,
    BigDecimal cantidadKg,
    BigDecimal precioUnitarioKgCop,
    boolean pendientePrecio,
    String referenciaOrigen,
    Instant fechaHoraSync) {}
