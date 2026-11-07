package co.edu.unimagdalena.avicontrol.infrastructure.adapter.export;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.CategoriaDesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.IngresosDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.PartidaDesgloseDto;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Plan 004, T009: el libro trae los valores de la pantalla y fórmulas sobre el rango real. */
class ExcelWorkbookBuilderTest {

  private static final int SUBTOTAL = 5;

  private final ExcelWorkbookBuilder constructor = new ExcelWorkbookBuilder();

  @Test
  void escribeLaMatrizDeVentaFinalConLosIndicadores() throws IOException {
    try (var libro = leer(desglose(ingresos(), false))) {
      var hoja = libro.getSheet(ExcelWorkbookBuilder.HOJA);

      assertThat(texto(hoja, "Lote")).isEqualTo("L-2026-03");
      assertThat(texto(hoja, "Galpón")).isEqualTo("Galpón Este");
      assertThat(texto(hoja, "Estado")).isEqualTo("ACTIVA");
      assertThat(numero(hoja, "Pollos vendidos")).isEqualTo(8500);
      assertThat(numero(hoja, "Peso total (kg)")).isEqualTo(23_800);
      assertThat(numero(hoja, "Peso promedio (kg)")).isEqualTo(2.8);
      assertThat(numero(hoja, "Precio por kg (COP)")).isEqualTo(4500);
      assertThat(numero(hoja, "Venta Bruta")).isEqualTo(107_100_000);
      assertThat(numero(hoja, "Mortalidad del Lote (aves)")).isEqualTo(500);
      assertThat(numero(hoja, "Mortalidad del Lote (%)")).isEqualTo(5.56);
      assertThat(numero(hoja, "Costos Operativos")).isEqualTo(85_000_000);
      assertThat(numero(hoja, "Utilidad Neta")).isEqualTo(22_100_000);
    }
  }

  @Test
  void cadaPartidaConservaSusValoresYSuOrigen() throws IOException {
    try (var libro = leer(desglose(ingresos(), false))) {
      var hoja = libro.getSheet(ExcelWorkbookBuilder.HOJA);
      var fila = fila(hoja, "Amoxicilina");

      assertThat(fila.getCell(1).getNumericCellValue()).isEqualTo(30.5);
      assertThat(fila.getCell(2).getStringCellValue()).isEqualTo("ML");
      assertThat(fila.getCell(3).getNumericCellValue()).isEqualTo(20_000);
      assertThat(fila.getCell(4).getStringCellValue())
          .isEqualTo("Módulo 2 / Consumo de medicamento · REC-090");
      assertThat(fila.getCell(SUBTOTAL).getNumericCellValue()).isEqualTo(2_200_000);
    }
  }

  @Test
  void losSubtotalesYElTotalSonFormulasSobreLasPartidas() throws IOException {
    try (var libro = leer(desglose(ingresos(), false))) {
      var hoja = libro.getSheet(ExcelWorkbookBuilder.HOJA);
      var alimento = fila(hoja, "Subtotal Alimento").getCell(SUBTOTAL);
      var total = fila(hoja, "TOTAL COSTOS OPERATIVOS").getCell(SUBTOTAL);
      int inicio = fila(hoja, "Pre-inicio").getRowNum() + 1;

      assertThat(alimento.getCellType()).isEqualTo(CellType.FORMULA);
      assertThat(alimento.getCellFormula()).isEqualTo("SUM(F%d:F%d)".formatted(inicio, inicio + 1));
      assertThat(alimento.getNumericCellValue()).isEqualTo(64_800_000);
      assertThat(total.getCellFormula()).startsWith("SUM(F").contains(",");
      assertThat(total.getNumericCellValue()).isEqualTo(85_000_000);
      assertThat(total.getCellStyle().getDataFormatString())
          .isEqualTo(ExcelWorkbookBuilder.FORMATO_COP);
    }
  }

  @Test
  void unaCategoriaSinPartidasNoRompeElTotal() throws IOException {
    var sinInsumos =
        List.of(
            categorias().get(0),
            new CategoriaDesgloseDto("MEDICINA", "Insumos Médicos", 0, List.of()),
            categorias().get(2));

    try (var libro = leer(desglose(ingresos(), false, sinInsumos))) {
      var hoja = libro.getSheet(ExcelWorkbookBuilder.HOJA);

      assertThat(fila(hoja, "Subtotal Insumos Médicos").getCell(SUBTOTAL).getNumericCellValue())
          .isZero();
      assertThat(fila(hoja, "TOTAL COSTOS OPERATIVOS").getCell(SUBTOTAL).getNumericCellValue())
          .isEqualTo(82_800_000);
    }
  }

  @Test
  void elSiniestroTotalIndicaQueNoHuboVenta() throws IOException {
    var sinVenta = new IngresosDto(0, new BigDecimal("0.00"), null, null, 0);

    try (var libro = leer(desglose(sinVenta, true))) {
      var hoja = libro.getSheet(ExcelWorkbookBuilder.HOJA);

      assertThat(texto(hoja, "Siniestro total")).isEqualTo("El lote no registró venta");
      assertThat(texto(hoja, "Peso promedio (kg)")).isEqualTo("No aplica");
      assertThat(texto(hoja, "Precio por kg (COP)")).isEqualTo("No aplica");
      assertThat(numero(hoja, "Venta Bruta")).isZero();
    }
  }

  @Test
  void unConceptoConSignoIgualSeGuardaComoTexto() throws IOException {
    var partida =
        new PartidaDesgloseDto(
            "=HYPERLINK(\"http://x\")",
            BigDecimal.ONE,
            "KG",
            BigDecimal.TEN,
            10,
            "MODULO_2",
            "REC-1");
    var malicioso = List.of(new CategoriaDesgloseDto("ALIMENTO", "Alimento", 10, List.of(partida)));

    try (var libro = leer(desglose(ingresos(), false, malicioso))) {
      var celda = fila(libro.getSheet(ExcelWorkbookBuilder.HOJA), "=HYPERLINK").getCell(0);

      assertThat(celda.getCellType()).isEqualTo(CellType.STRING);
    }
  }

  private XSSFWorkbook leer(DesgloseDto desglose) throws IOException {
    return new XSSFWorkbook(new ByteArrayInputStream(constructor.generar(desglose)));
  }

  private static Row fila(Sheet hoja, String rotulo) {
    for (Row fila : hoja) {
      Cell celda = fila.getCell(0);
      if (celda != null
          && celda.getCellType() == CellType.STRING
          && celda.getStringCellValue().startsWith(rotulo)) {
        return fila;
      }
    }
    throw new AssertionError("No está la fila " + rotulo);
  }

  private static String texto(Sheet hoja, String rotulo) {
    return fila(hoja, rotulo).getCell(1).getStringCellValue();
  }

  private static double numero(Sheet hoja, String rotulo) {
    return fila(hoja, rotulo).getCell(1).getNumericCellValue();
  }

  private static IngresosDto ingresos() {
    return new IngresosDto(
        8500,
        new BigDecimal("23800.00"),
        new BigDecimal("2.80"),
        new BigDecimal("4500.00"),
        107_100_000);
  }

  private static List<CategoriaDesgloseDto> categorias() {
    return List.of(
        new CategoriaDesgloseDto(
            "ALIMENTO",
            "Alimento",
            64_800_000,
            List.of(
                partida("Pre-inicio", "6000.000", "KG", "2600.00", 15_600_000, "REC-1"),
                partida("Inicio", "19680.000", "KG", "2500.00", 49_200_000, "REC-2"))),
        new CategoriaDesgloseDto(
            "MEDICINA",
            "Insumos Médicos",
            2_200_000,
            List.of(partida("Amoxicilina", "30.500", "ML", "20000.00", 2_200_000, "REC-090"))),
        new CategoriaDesgloseDto(
            "POBLACION",
            "Costo de Población",
            18_000_000,
            List.of(partida("Costo del lote", "1", "LOTE", "18000000.00", 18_000_000, "uuid"))));
  }

  private static PartidaDesgloseDto partida(
      String concepto,
      String cantidad,
      String unidad,
      String precio,
      long subtotal,
      String referencia) {
    return new PartidaDesgloseDto(
        concepto,
        new BigDecimal(cantidad),
        unidad,
        new BigDecimal(precio),
        subtotal,
        "MODULO_2",
        referencia);
  }

  private static DesgloseDto desglose(IngresosDto ingresos, boolean siniestro) {
    return desglose(ingresos, siniestro, categorias());
  }

  private static DesgloseDto desglose(
      IngresosDto ingresos, boolean siniestro, List<CategoriaDesgloseDto> categorias) {
    return new DesgloseDto(
        42L,
        "ACTIVA",
        UUID.randomUUID(),
        "L-2026-03",
        "Galpón Este",
        siniestro,
        ingresos,
        categorias,
        500,
        new BigDecimal("5.56"),
        85_000_000,
        22_100_000,
        null,
        true);
  }
}
