package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;
import co.edu.unimagdalena.avicontrol.domain.model.SubtotalesCategoria;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Liquidación generada o consultada (CU03.FR-013 y FR-014), con los valores congelados. */
public record LiquidacionDto(
    long idLiquidacion,
    String estado,
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
    Instant fechaHoraGeneracion,
    String usuarioResponsable,
    Instant fechaSincronizacionM1,
    Instant fechaSincronizacionM2,
    RegistroAnulacion registroAnulacion,
    List<String> accionesDisponibles) {}
