package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import co.edu.unimagdalena.avicontrol.domain.model.SubtotalesCategoria;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Vista previa de la Liquidación (CU03.FR-015, paso 2). No se persiste ni tiene estado. */
public record LiquidacionPreviaDto(
    boolean esVistaPrevia,
    String aviso,
    UUID idLote,
    UUID idGalpon,
    String nombreLote,
    String nombreGalpon,
    boolean siniestroTotal,
    BigDecimal precioKgCop,
    int pollosVendidos,
    BigDecimal pesoTotalKg,
    BigDecimal pesoPromedioKg,
    long ventaBrutaCop,
    int mortalidadAves,
    BigDecimal porcentajeMortalidad,
    SubtotalesCategoria subtotalesCategoria,
    long costosOperativosCop,
    long utilidadNetaCop,
    Instant fechaSincronizacionM1,
    Instant fechaSincronizacionM2,
    String huellaDatos) {}
