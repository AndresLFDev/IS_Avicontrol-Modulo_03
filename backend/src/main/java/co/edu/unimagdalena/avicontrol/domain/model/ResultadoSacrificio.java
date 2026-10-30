package co.edu.unimagdalena.avicontrol.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Resultado final de sacrificio sincronizado desde Módulo 2 (M3-CU08). */
public record ResultadoSacrificio(
    UUID idResultado, int cantidadFinalPollos, BigDecimal pesoTotalKg, Instant fechaHoraSync) {}
