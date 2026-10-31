package co.edu.unimagdalena.avicontrol.domain.model;

import co.edu.unimagdalena.avicontrol.domain.shared.Rounding;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Copia local sincronizada que sustenta una Liquidación (CU03.FR-009): el lote de Módulo 1, el
 * resultado de sacrificio (nulo en siniestro total) y las partidas de alimento y medicamento de
 * Módulo 2. Ya pasó las precondiciones de la generación.
 */
public record DatosLiquidacion(
    LoteLiquidable lote,
    ResultadoSacrificio resultado,
    List<PartidaAlimento> alimento,
    List<TramoMedicamento> medicamentos) {

  public DatosLiquidacion {
    alimento = List.copyOf(alimento);
    medicamentos = List.copyOf(medicamentos);
  }

  public Optional<ResultadoSacrificio> resultadoSacrificio() {
    return Optional.ofNullable(resultado);
  }

  public boolean siniestroTotal() {
    return lote.siniestroTotal();
  }

  /** Partidas sin precio aplicable (CU03.FR-006). */
  public static List<PartidaPendiente> partidasPendientes(
      List<PartidaAlimento> alimento, List<TramoMedicamento> medicamentos) {
    var pendientes = new ArrayList<PartidaPendiente>();
    alimento.stream()
        .filter(PartidaAlimento::pendientePrecio)
        .map(
            p ->
                new PartidaPendiente(
                    CategoriaCosto.ALIMENTO, p.tipoAlimento(), p.referenciaOrigen()))
        .forEach(pendientes::add);
    medicamentos.stream()
        .filter(TramoMedicamento::pendientePrecio)
        .map(
            t ->
                new PartidaPendiente(
                    CategoriaCosto.MEDICINA, t.medicamento(), t.referenciaRecepcion()))
        .forEach(pendientes::add);
    return pendientes;
  }

  /**
   * Partidas valorizadas según las reglas de llenado de {@code partida_costo_lote} (arquitectura
   * general, sección C): una por partida de alimento, una por tramo de medicamento y una sola de
   * costo de población, para que su subtotal cuadre al peso con Módulo 1.
   */
  public List<PartidaCosto> partidasCosto() {
    var partidas = new ArrayList<PartidaCosto>();
    for (var p : alimento) {
      partidas.add(
          new PartidaCosto(
              CategoriaCosto.ALIMENTO,
              p.tipoAlimento(),
              p.cantidadKg(),
              "KG",
              p.precioUnitarioKgCop(),
              Rounding.cop(p.cantidadKg().multiply(p.precioUnitarioKgCop())),
              FuenteSincronizacion.MODULO_2,
              p.referenciaOrigen()));
    }
    for (var t : medicamentos) {
      partidas.add(
          new PartidaCosto(
              CategoriaCosto.MEDICINA,
              t.medicamento(),
              t.cantidadUnidadBase(),
              t.unidadBase(),
              t.precioUnitarioBaseCop(),
              Rounding.cop(t.cantidadUnidadBase().multiply(t.precioUnitarioBaseCop())),
              FuenteSincronizacion.MODULO_2,
              t.referenciaRecepcion()));
    }
    partidas.add(
        new PartidaCosto(
            CategoriaCosto.POBLACION,
            "Costo de adquisición del lote (" + lote.poblacionInicial() + " aves)",
            BigDecimal.ONE,
            "LOTE",
            BigDecimal.valueOf(lote.costoTotalCop()),
            lote.costoTotalCop(),
            FuenteSincronizacion.MODULO_1,
            null));
    return partidas;
  }

  /** Fecha y hora de sincronización de Módulo 1 que se congela en el snapshot. */
  public Instant fechaSincronizacionM1() {
    return lote.fechaHoraSync();
  }

  /** La más reciente entre el resultado y las partidas de Módulo 2 usadas en el cálculo. */
  public Instant fechaSincronizacionM2() {
    return Stream.of(
            resultadoSacrificio().map(ResultadoSacrificio::fechaHoraSync).stream(),
            alimento.stream().map(PartidaAlimento::fechaHoraSync),
            medicamentos.stream().map(TramoMedicamento::fechaHoraSync))
        .flatMap(s -> s)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  /**
   * SHA-256 de los datos usados en el cálculo y del precio (arquitectura general, "Concurrencia").
   * La confirmación la compara con la de la vista previa para persistir exactamente lo mostrado.
   */
  public String huella(BigDecimal precioKgCop) {
    var texto = new StringBuilder();
    agregar(
        texto,
        "lote",
        lote.idLote(),
        lote.poblacionInicial(),
        lote.poblacionActual(),
        lote.costoTotalCop(),
        lote.fechaHoraSync());
    resultadoSacrificio()
        .ifPresent(
            r ->
                agregar(
                    texto,
                    "resultado",
                    r.idResultado(),
                    r.cantidadFinalPollos(),
                    numero(r.pesoTotalKg()),
                    r.fechaHoraSync()));
    alimento.stream()
        .sorted(Comparator.comparing(PartidaAlimento::idPartidaOrigen))
        .forEach(
            p ->
                agregar(
                    texto,
                    "alimento",
                    p.idPartidaOrigen(),
                    numero(p.cantidadKg()),
                    numero(p.precioUnitarioKgCop()),
                    p.fechaHoraSync()));
    medicamentos.stream()
        .sorted(
            Comparator.comparing(TramoMedicamento::idConsumoOrigen)
                .thenComparing(TramoMedicamento::referenciaRecepcion))
        .forEach(
            t ->
                agregar(
                    texto,
                    "medicamento",
                    t.idConsumoOrigen(),
                    t.referenciaRecepcion(),
                    numero(t.cantidadUnidadBase()),
                    numero(t.precioUnitarioBaseCop()),
                    t.fechaHoraSync()));
    agregar(texto, "precio", numero(precioKgCop));
    try {
      var sha = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(sha.digest(texto.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("La JVM no ofrece SHA-256", e);
    }
  }

  private static void agregar(StringBuilder texto, Object... valores) {
    for (var valor : valores) {
      texto.append(Objects.toString(valor, "-")).append('|');
    }
    texto.append('\n');
  }

  /** Representación sin ceros a la derecha, para que 23800.00 y 23800 den la misma huella. */
  private static String numero(BigDecimal valor) {
    return valor == null ? null : valor.stripTrailingZeros().toPlainString();
  }
}
