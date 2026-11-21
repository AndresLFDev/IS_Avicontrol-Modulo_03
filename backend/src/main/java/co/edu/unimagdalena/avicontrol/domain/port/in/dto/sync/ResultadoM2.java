package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Resultado final de sacrificio de un lote, como lo entrega Módulo 2 (M3-CU08). */
public record ResultadoM2(
    UUID id,
    UUID loteId,
    UUID galponId,
    Integer cantidadFinalPollos,
    BigDecimal pesoTotalKg,
    Instant fechaRegistro) {}
