package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Proyección de solo lectura de una Liquidación: no se almacena (CU05.FR-007). Pantalla y Excel
 * salen de este mismo objeto, por lo que muestran los mismos valores (CU05.SC-001).
 */
public record DesgloseDto(
    long idLiquidacion,
    String estado,
    UUID idLote,
    String nombreLote,
    String nombreGalpon,
    boolean siniestroTotal,
    IngresosDto ingresos,
    List<CategoriaDesgloseDto> categorias,
    int mortalidadAves,
    BigDecimal porcentajeMortalidad,
    long costosOperativosCop,
    long utilidadNetaCop,
    RegistroAnulacion registroAnulacion,
    boolean exportacionPermitida) {}
