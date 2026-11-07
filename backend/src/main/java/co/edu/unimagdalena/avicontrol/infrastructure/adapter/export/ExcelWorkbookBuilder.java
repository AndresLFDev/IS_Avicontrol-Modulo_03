package co.edu.unimagdalena.avicontrol.infrastructure.adapter.export;

import co.edu.unimagdalena.avicontrol.domain.exception.ExportacionFallidaException;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.CategoriaDesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.PartidaDesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.GeneradorExcelPort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Libro de una hoja con la información del lote, la Matriz de Venta Final y el desglose de costos
 * (CU05.FR-003). Cada exportación usa su propio libro en memoria, sin estado compartido, para que
 * varias simultáneas no se bloqueen ni se mezclen.
 */
@Component
public class ExcelWorkbookBuilder implements GeneradorExcelPort {

  static final String HOJA = "Liquidacion_Lote";
  static final String FORMATO_COP = "$#,##0";
  private static final String FORMATO_PRECIO = "$#,##0.00";
  private static final int COLUMNA_CANTIDAD = 1;
  private static final int COLUMNA_UNIDAD = 2;
  private static final int COLUMNA_PRECIO = 3;
  private static final int COLUMNA_ORIGEN = 4;
  private static final int COLUMNA_SUBTOTAL = 5;
  private static final String LETRA_SUBTOTAL = "F";
  private static final int[] ANCHOS = {44, 14, 10, 22, 54, 18};
  private static final String[] ENCABEZADOS = {
    "Concepto", "Cantidad", "Unidad", "Precio unitario (COP)", "Origen", "Subtotal (COP)"
  };

  @Override
  public byte[] generar(DesgloseDto desglose) {
    try (var libro = new XSSFWorkbook();
        var salida = new ByteArrayOutputStream()) {
      var hoja = libro.createSheet(HOJA);
      for (int i = 0; i < ANCHOS.length; i++) {
        hoja.setColumnWidth(i, ANCHOS[i] * 256);
      }
      var estilos = new Estilos(libro);
      int fila = escribirMatriz(hoja, desglose, estilos);
      escribirCostos(hoja, desglose, estilos, fila + 1);

      // Guarda el resultado de las fórmulas para los visores que no las recalculan
      libro.getCreationHelper().createFormulaEvaluator().evaluateAll();
      libro.write(salida);
      return salida.toByteArray();
    } catch (IOException e) {
      throw new ExportacionFallidaException(desglose.idLiquidacion(), e);
    }
  }

  private static int escribirMatriz(Sheet hoja, DesgloseDto d, Estilos estilos) {
    var ingresos = d.ingresos();
    int fila = 0;
    texto(hoja.createRow(fila++), 0, "Liquidación de lote", estilos.titulo);
    fila = dato(hoja, fila, "Liquidación N.º", d.idLiquidacion(), estilos.entero);
    fila = dato(hoja, fila, "Lote", d.nombreLote());
    fila = dato(hoja, fila, "Galpón", d.nombreGalpon());
    fila = dato(hoja, fila, "Estado", d.estado());
    fila++;

    texto(hoja.createRow(fila++), 0, "Matriz de Venta Final", estilos.negrilla);
    if (d.siniestroTotal()) {
      fila = dato(hoja, fila, "Siniestro total", "El lote no registró venta");
    }
    fila = dato(hoja, fila, "Pollos vendidos", ingresos.pollosVendidos(), estilos.entero);
    fila = dato(hoja, fila, "Peso total (kg)", ingresos.pesoTotalKg(), estilos.decimal);
    fila = dato(hoja, fila, "Peso promedio (kg)", ingresos.pesoPromedioKg(), estilos.decimal);
    fila = dato(hoja, fila, "Precio por kg (COP)", ingresos.precioKgCop(), estilos.precio);
    fila = dato(hoja, fila, "Venta Bruta", ingresos.ventaBrutaCop(), estilos.cop);
    fila = dato(hoja, fila, "Mortalidad del Lote (aves)", d.mortalidadAves(), estilos.entero);
    fila = dato(hoja, fila, "Mortalidad del Lote (%)", d.porcentajeMortalidad(), estilos.decimal);
    fila = dato(hoja, fila, "Costos Operativos", d.costosOperativosCop(), estilos.cop);
    return dato(hoja, fila, "Utilidad Neta", d.utilidadNetaCop(), estilos.cop);
  }

  private static void escribirCostos(Sheet hoja, DesgloseDto d, Estilos estilos, int inicio) {
    int fila = inicio;
    texto(hoja.createRow(fila++), 0, "Desglose de costos", estilos.negrilla);
    var encabezado = hoja.createRow(fila++);
    for (int i = 0; i < ENCABEZADOS.length; i++) {
      texto(encabezado, i, ENCABEZADOS[i], estilos.encabezado);
    }

    var subtotales = new ArrayList<String>();
    for (var categoria : d.categorias()) {
      fila = escribirCategoria(hoja, categoria, estilos, fila);
      subtotales.add(LETRA_SUBTOTAL + fila);
    }

    var total = hoja.createRow(fila);
    texto(total, 0, "TOTAL COSTOS OPERATIVOS", estilos.negrilla);
    suma(total, String.join(",", subtotales), estilos.copNegrilla);
  }

  /** Devuelve la fila siguiente; la inmediatamente anterior es el subtotal de la categoría. */
  private static int escribirCategoria(
      Sheet hoja, CategoriaDesgloseDto categoria, Estilos estilos, int inicio) {
    int fila = inicio;
    texto(hoja.createRow(fila++), 0, categoria.etiqueta(), estilos.negrilla);
    int primera = fila;
    for (var partida : categoria.partidas()) {
      escribirPartida(hoja.createRow(fila++), categoria.categoria(), partida, estilos);
    }

    var subtotal = hoja.createRow(fila);
    texto(subtotal, 0, "Subtotal " + categoria.etiqueta(), estilos.negrilla);
    if (categoria.partidas().isEmpty()) {
      numero(subtotal, COLUMNA_SUBTOTAL, 0, estilos.copNegrilla);
    } else {
      suma(
          subtotal,
          LETRA_SUBTOTAL + (primera + 1) + ":" + LETRA_SUBTOTAL + fila,
          estilos.copNegrilla);
    }
    return fila + 1;
  }

  private static void escribirPartida(
      Row fila, String categoria, PartidaDesgloseDto partida, Estilos estilos) {
    texto(fila, 0, partida.concepto(), null);
    numero(fila, COLUMNA_CANTIDAD, partida.cantidad().doubleValue(), estilos.cantidad);
    texto(fila, COLUMNA_UNIDAD, partida.unidadMedida(), null);
    numero(fila, COLUMNA_PRECIO, partida.precioUnitarioCop().doubleValue(), estilos.precio);
    texto(fila, COLUMNA_ORIGEN, origen(categoria, partida), null);
    numero(fila, COLUMNA_SUBTOTAL, partida.subtotalCop(), estilos.cop);
  }

  /** Misma trazabilidad de la pantalla: módulo, tipo de dato y referencia de la fuente. */
  private static String origen(String categoria, PartidaDesgloseDto partida) {
    String fuente =
        switch (categoria) {
          case "ALIMENTO" -> "Módulo 2 / Partida de alimento";
          case "MEDICINA" -> "Módulo 2 / Consumo de medicamento";
          default -> "Módulo 1 / Costo total del lote";
        };
    return fuente + " · " + partida.referenciaOrigen();
  }

  private static void suma(Row fila, String celdas, CellStyle estilo) {
    Cell celda = fila.createCell(COLUMNA_SUBTOTAL);
    celda.setCellFormula("SUM(" + celdas + ")");
    celda.setCellStyle(estilo);
  }

  private static int dato(Sheet hoja, int fila, String titulo, String valor) {
    var renglon = hoja.createRow(fila);
    texto(renglon, 0, titulo, null);
    texto(renglon, 1, valor, null);
    return fila + 1;
  }

  private static int dato(Sheet hoja, int fila, String titulo, long valor, CellStyle estilo) {
    var renglon = hoja.createRow(fila);
    texto(renglon, 0, titulo, null);
    numero(renglon, 1, valor, estilo);
    return fila + 1;
  }

  /** Sin venta no hay promedio ni precio: se escribe "No aplica" en vez de un cero engañoso. */
  private static int dato(Sheet hoja, int fila, String titulo, BigDecimal valor, CellStyle estilo) {
    if (valor == null) {
      return dato(hoja, fila, titulo, "No aplica");
    }
    var renglon = hoja.createRow(fila);
    texto(renglon, 0, titulo, null);
    numero(renglon, 1, valor.doubleValue(), estilo);
    return fila + 1;
  }

  private static void texto(Row fila, int columna, String valor, CellStyle estilo) {
    Cell celda = fila.createCell(columna);
    celda.setCellValue(valor);
    if (estilo != null) {
      celda.setCellStyle(estilo);
    }
  }

  private static void numero(Row fila, int columna, double valor, CellStyle estilo) {
    Cell celda = fila.createCell(columna);
    celda.setCellValue(valor);
    celda.setCellStyle(estilo);
  }

  /** Un libro admite un número limitado de estilos: se crean una vez y se reutilizan. */
  private static final class Estilos {

    final CellStyle titulo;
    final CellStyle encabezado;
    final CellStyle negrilla;
    final CellStyle cop;
    final CellStyle copNegrilla;
    final CellStyle precio;
    final CellStyle cantidad;
    final CellStyle decimal;
    final CellStyle entero;

    Estilos(XSSFWorkbook libro) {
      var fuenteNegrilla = libro.createFont();
      fuenteNegrilla.setBold(true);
      var fuenteTitulo = libro.createFont();
      fuenteTitulo.setBold(true);
      fuenteTitulo.setFontHeightInPoints((short) 14);

      titulo = libro.createCellStyle();
      titulo.setFont(fuenteTitulo);
      negrilla = libro.createCellStyle();
      negrilla.setFont(fuenteNegrilla);
      encabezado = libro.createCellStyle();
      encabezado.setFont(fuenteNegrilla);
      encabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      var formatos = libro.createDataFormat();
      cop = libro.createCellStyle();
      cop.setDataFormat(formatos.getFormat(FORMATO_COP));
      copNegrilla = libro.createCellStyle();
      copNegrilla.cloneStyleFrom(cop);
      copNegrilla.setFont(fuenteNegrilla);
      precio = libro.createCellStyle();
      precio.setDataFormat(formatos.getFormat(FORMATO_PRECIO));
      cantidad = libro.createCellStyle();
      cantidad.setDataFormat(formatos.getFormat("#,##0.000"));
      decimal = libro.createCellStyle();
      decimal.setDataFormat(formatos.getFormat("#,##0.00"));
      entero = libro.createCellStyle();
      entero.setDataFormat(formatos.getFormat("#,##0"));
    }
  }
}
