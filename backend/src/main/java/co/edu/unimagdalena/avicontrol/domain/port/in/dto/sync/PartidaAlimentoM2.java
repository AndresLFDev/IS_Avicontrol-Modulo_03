package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Suministro real de alimento de un lote, separado por recepción (M3-CU09). Se identifica por el
 * par {@code (suministroId, recepcionId)}: un suministro que abarca dos recepciones llega como dos
 * partidas con precios distintos.
 */
public record PartidaAlimentoM2(
    UUID suministroId,
    UUID recepcionId,
    String nombreAlimento,
    String tipoAlimento,
    BigDecimal cantidadKg,
    BigDecimal precioNetoKg,
    BigDecimal valorImpuestoCop) {}
