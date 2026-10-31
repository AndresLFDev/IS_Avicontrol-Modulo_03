package co.edu.unimagdalena.avicontrol.domain.model;

import co.edu.unimagdalena.avicontrol.domain.shared.Rounding;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Indicadores de la Matriz de Venta Final (CU03.FR-001 a FR-004 y FR-012). La Venta Bruta usa el
 * peso total, nunca el promedio; la mortalidad sale de las poblaciones de Módulo 1, no de los
 * pollos vendidos.
 */
public record CalculoLiquidacion(
    int pollosVendidos,
    BigDecimal pesoTotalKg,
    BigDecimal pesoPromedioKg,
    BigDecimal precioKgCop,
    long ventaBrutaCop,
    int mortalidadAves,
    BigDecimal porcentajeMortalidad,
    SubtotalesCategoria subtotales,
    long costosOperativosCop,
    long utilidadNetaCop,
    List<PartidaCosto> partidas) {

  private static final BigDecimal PESO_CERO = BigDecimal.ZERO.setScale(2);

  public CalculoLiquidacion {
    partidas = List.copyOf(partidas);
  }

  /** {@code precioKgCop} es nulo en siniestro total. */
  public static CalculoLiquidacion de(DatosLiquidacion datos, BigDecimal precioKgCop) {
    var lote = datos.lote();
    int pollos = 0;
    BigDecimal peso = PESO_CERO;
    BigDecimal promedio = null;
    long ventaBruta = 0;
    if (!datos.siniestroTotal()) {
      var resultado = datos.resultadoSacrificio().orElseThrow();
      pollos = resultado.cantidadFinalPollos();
      peso = resultado.pesoTotalKg();
      promedio =
          pollos == 0 ? null : peso.divide(BigDecimal.valueOf(pollos), 2, RoundingMode.HALF_UP);
      ventaBruta = Rounding.cop(peso.multiply(precioKgCop));
    }

    int mortalidad = lote.poblacionInicial() - lote.poblacionActual();
    var partidas = datos.partidasCosto();
    var subtotales =
        new SubtotalesCategoria(
            sumar(partidas, CategoriaCosto.ALIMENTO),
            sumar(partidas, CategoriaCosto.MEDICINA),
            sumar(partidas, CategoriaCosto.POBLACION));
    long costos = subtotales.total();

    return new CalculoLiquidacion(
        pollos,
        peso,
        promedio,
        datos.siniestroTotal() ? null : precioKgCop,
        ventaBruta,
        mortalidad,
        Rounding.porcentaje(mortalidad, lote.poblacionInicial()),
        subtotales,
        costos,
        ventaBruta - costos,
        partidas);
  }

  private static long sumar(List<PartidaCosto> partidas, CategoriaCosto categoria) {
    return partidas.stream()
        .filter(p -> p.categoria() == categoria)
        .mapToLong(PartidaCosto::subtotalCop)
        .sum();
  }
}
