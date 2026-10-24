# Implementation Plan: Historial de Liquidaciones y Exportación de Desglose

**Date**: 2026-09-28  
**Actualizado**: 2026-10-23  
**Arquitectura y catálogo de errores**: [general.md](../arquitectura/general.md)  
**Specs**:
- [m3-cu05-desglose-ventas-gastos](../features/m3-cu05-desglose-ventas-gastos/spec.md) – Consultar Desglose de Ventas y Gastos  
- [m3-cu06-historial-liquidaciones](../features/m3-cu06-historial-liquidaciones/spec.md) – Consultar Historial de Liquidaciones  

## Summary

Este plan aborda la **consulta histórica cronológica** de liquidaciones (`CU06`) y la **visualización y exportación en Excel del desglose pormenorizado de ventas y gastos** (`CU05`).

**Historial (CU06)**: Permite al Administrador Financiero buscar liquidaciones por nombre o UUID de galpón o de lote y filtrarlas por rango de fechas (orden descendente). Es el **único punto de acceso** a las Liquidaciones ya generadas, porque la lista de lotes no muestra los lotes con Liquidación `ACTIVA` (M3-CU01.FR-014). Al seleccionar cualquier fila, el sistema abre la **vista de la Liquidación** (M3-CU03.FR-014), que muestra el registro de anulación si está `ANULADA` — no el desglose directamente. El Desglose se consulta desde la propia vista de la Liquidación (CU06.FR-006).

**Desglose (CU05)**: Visualización de la sección de ingresos y de las partidas agrupadas por categorías de costo — **Alimento**, **Insumos Médicos** y **Costo de Población** — garantizando que los subtotales cuadran exactamente con `costosOperativosCop` de la Liquidación (FR-002). Exportación a `.xlsx` con Apache POI (`poi-ooxml:5.2.5`), solo para Liquidaciones `ACTIVA`.

## Technical Context

- **Language/Version**: Java 21 (LTS)
- **Primary Dependencies**: Spring Boot 3.3.x, Spring Data JPA, Spring Web MVC, Apache POI `poi-ooxml:5.2.5`, JUnit 5, MockMvc
- **Storage**: Proyección y lectura sobre tablas `liquidacion`, `partida_costo_lote`, `registro_anulacion`, `lote`, `galpon`
- **Testing**: Unit tests de lógica de proyección/agrupación, tests de generación de Excel con Apache POI y tests REST
- **Performance Goals**: Generación y descarga de archivo Excel < 10s (CU05.SC-002); consulta de historial < 3s (CU06.SC-001)

---

## Contrato REST del Historial (CU06)

### `GET /api/v1/liquidaciones`

| Query param | Tipo | Obligatorio | Valor por defecto | Regla |
|---|---|---|---|---|
| `search` | string | No | — | Texto libre, sin distinguir mayúsculas: coincide por contenido con nombre de galpón o de lote, o exacto con UUID de galpón o de lote (FR-003). Omitido = todas. Máx. 100 caracteres |
| `desde` | fecha ISO `yyyy-MM-dd` | No | — | Incluye las Liquidaciones generadas desde las 00:00 de ese día (zona `America/Bogota`) |
| `hasta` | fecha ISO `yyyy-MM-dd` | No | — | Incluye hasta las 23:59:59.999 de ese día (zona `America/Bogota`). Si `desde > hasta` → `400` `rango-fechas-invalido` (FR-005) |
| `page` | int | No | `0` | Índice 0-based, `>= 0`; si no, `400` |
| `size` | int | No | `10` | `1..50`; fuera de ese rango → `400` |

- Orden fijo: `fechaHoraGeneracion` descendente (FR-001). No se expone `sort`.
- Incluye Liquidaciones `ACTIVA` y `ANULADA` (FR-003).
- Respuestas: `200` (incluso con `content` vacío) y `400` (rango o parámetros inválidos). Ver el catálogo de errores en la arquitectura general.

```json
GET /api/v1/liquidaciones?search=norte&desde=2026-07-01&hasta=2026-09-30&page=0&size=10

HTTP/1.1 200 OK
{
  "content": [
    {
      "idLiquidacion": 42,
      "fechaHoraGeneracion": "2026-09-28T16:42:00Z",
      "idGalpon": "f1e2d3c4-e89b-12d3-a456-426614174000",
      "nombreGalpon": "Galpón Norte",
      "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
      "nombreLote": "Lote Norte Ciclo 4",
      "ventaBrutaCop": 139200000,
      "porcentajeMortalidad": 4.00,
      "costosOperativosCop": 60200000,
      "utilidadNetaCop": 79000000,
      "estado": "ACTIVA"
    },
    {
      "idLiquidacion": 40,
      "fechaHoraGeneracion": "2026-09-20T09:15:00Z",
      "idGalpon": "f1e2d3c4-e89b-12d3-a456-426614174000",
      "nombreGalpon": "Galpón Norte",
      "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
      "nombreLote": "Lote Norte Ciclo 4",
      "ventaBrutaCop": 132000000,
      "porcentajeMortalidad": 4.00,
      "costosOperativosCop": 60200000,
      "utilidadNetaCop": 71800000,
      "estado": "ANULADA"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1,
  "mensaje": null
}
```

Cuando `content` está vacío, `mensaje` trae el texto de FR-007: `"No hay liquidaciones registradas aún"` si no existe ninguna Liquidación y no se envió ningún filtro; `"No se encontraron liquidaciones para los criterios de búsqueda aplicados"` en cualquier otro caso.

El registro de anulación (motivo, fecha, hora y responsable) de una fila `ANULADA` se consulta abriendo la fila, que lleva a `GET /api/v1/liquidaciones/{id}` (CU06.FR-004, CU04.FR-010).

```json
GET /api/v1/liquidaciones?desde=2026-09-30&hasta=2026-09-01

HTTP/1.1 400 Bad Request
{
  "type": "https://avicontrol.edu.co/errors/rango-fechas-invalido",
  "title": "Rango de fechas inválido",
  "status": 400,
  "detail": "La fecha inicial (2026-09-30) debe ser menor o igual a la fecha final (2026-09-01).",
  "instance": "/api/v1/liquidaciones",
  "timestamp": "2026-10-03T12:10:00Z"
}
```

---

## Contrato REST del Desglose (CU05)

### `GET /api/v1/liquidaciones/{id}/desglose`

```json
HTTP/1.1 200 OK
{
  "idLiquidacion": 42,
  "estado": "ACTIVA",
  "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
  "nombreLote": "Lote Norte Ciclo 4",
  "nombreGalpon": "Galpón Norte",
  "siniestroTotal": false,
  "ingresos": {
    "pollosVendidos": 9600,
    "pesoTotalKg": 24000.00,
    "pesoPromedioKg": 2.50,
    "precioKgCop": 5800.00,
    "ventaBrutaCop": 139200000
  },
  "categorias": [
    {
      "categoria": "ALIMENTO",
      "etiqueta": "Alimento",
      "subtotalCop": 45000000,
      "partidas": [
        { "concepto": "Engorde", "cantidad": 12500.000, "unidadMedida": "KG", "precioUnitarioCop": 2400.00, "subtotalCop": 30000000, "fuenteOrigen": "MODULO_2", "referenciaOrigen": "REC-2026-118" }
      ]
    },
    { "categoria": "MEDICINA", "etiqueta": "Insumos Médicos", "subtotalCop": 3200000, "partidas": [] },
    {
      "categoria": "POBLACION",
      "etiqueta": "Costo de Población",
      "subtotalCop": 12000000,
      "partidas": [
        { "concepto": "Costo de adquisición del lote (10000 aves)", "cantidad": 1, "unidadMedida": "LOTE", "precioUnitarioCop": 12000000.00, "subtotalCop": 12000000, "fuenteOrigen": "MODULO_1", "referenciaOrigen": "a1b2c3d4-e89b-12d3-a456-426614174000" }
      ]
    }
  ],
  "costosOperativosCop": 60200000,
  "utilidadNetaCop": 79000000,
  "registroAnulacion": null,
  "exportacionPermitida": true
}
```

- En siniestro total, `ingresos` trae `pollosVendidos = 0`, `pesoTotalKg = 0.00`, `ventaBrutaCop = 0` y `precioKgCop`/`pesoPromedioKg` en `null`; el frontend indica que no hubo venta (CU05 esc. 5).
- En una Liquidación `ANULADA`, `registroAnulacion` trae motivo, fecha, hora y responsable y `exportacionPermitida` es `false` (CU05.FR-004).
- La suma de `subtotalCop` de las categorías es igual a `costosOperativosCop` (FR-002); la prueba lo verifica.

### `GET /api/v1/liquidaciones/{id}/desglose/excel`

- `200` con el archivo `Liquidacion_Lote_<id>.xlsx`.
- `404 liquidacion-no-encontrada` si no existe.
- `409 exportacion-no-permitida` si la Liquidación está `ANULADA`.
- `500 error-interno` si falla la generación; el frontend muestra el error y ofrece reintentar sin perder la vista (CU05.FR-005).

---

## Snippets de Código Java de Puertos, DTOs, Servicio Apache POI y Controlador

### Puertos de Entrada
```java
package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialPageDto;
import java.time.LocalDate;

/** CU06 — Consultar historial cronológico filtrado */
public interface ConsultarHistorialUseCase {
    /**
     * @param search texto libre: nombre o UUID de galpón o de lote (null = todas)
     * @param desde  fecha inicial inclusiva (null = sin límite)
     * @param hasta  fecha final inclusiva (null = sin límite); desde > hasta → RangoFechasInvalidoException
     */
    HistorialPageDto consultarHistorial(String search, LocalDate desde, LocalDate hasta, int page, int size);
}

/** CU05 — Consultar proyección de desglose de ventas y gastos */
public interface ConsultarDesgloseUseCase {
    DesgloseDto consultarDesglose(Long idLiquidacion);
}

/** CU05 — Generar el reporte Excel (.xlsx); rechaza Liquidaciones ANULADA */
public interface ExportarExcelUseCase {
    byte[] generarReporteExcelLiquidacion(Long idLiquidacion);
}
```

### DTOs de Respuesta
```java
package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HistorialItemDto(
    Long idLiquidacion,
    Instant fechaHoraGeneracion,
    UUID idGalpon,
    String nombreGalpon,
    UUID idLote,
    String nombreLote,
    long ventaBrutaCop,
    BigDecimal porcentajeMortalidad,
    long costosOperativosCop,
    long utilidadNetaCop,
    String estado                 // "ACTIVA" | "ANULADA"
) {}

public record HistorialPageDto(
    List<HistorialItemDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    String mensaje                // null con resultados; texto de FR-007 cuando content está vacío
) {}

public record IngresosDto(
    int pollosVendidos,
    BigDecimal pesoTotalKg,
    BigDecimal pesoPromedioKg,    // null en siniestro total
    BigDecimal precioKgCop,       // null en siniestro total
    long ventaBrutaCop
) {}

public record PartidaDesgloseDto(
    String concepto,
    BigDecimal cantidad,
    String unidadMedida,
    BigDecimal precioUnitarioCop,
    long subtotalCop,
    String fuenteOrigen,          // "MODULO_1" | "MODULO_2" (trazabilidad, CA-G05)
    String referenciaOrigen
) {}

public record CategoriaDesgloseDto(
    String categoria,             // "ALIMENTO" | "MEDICINA" | "POBLACION"
    String etiqueta,              // "Alimento" | "Insumos Médicos" | "Costo de Población"
    long subtotalCop,
    List<PartidaDesgloseDto> partidas
) {}

public record DesgloseDto(
    Long idLiquidacion,
    String estado,
    UUID idLote,
    String nombreLote,
    String nombreGalpon,
    boolean siniestroTotal,
    IngresosDto ingresos,
    List<CategoriaDesgloseDto> categorias,
    long costosOperativosCop,
    long utilidadNetaCop,
    RegistroAnulacionDto registroAnulacion,   // null si está ACTIVA
    boolean exportacionPermitida              // false si está ANULADA
) {}
```

### Servicio de Exportación: `ExportarExcelService.java`

El servicio (capa `application`) arma el `DesgloseDto` y delega la construcción del libro al puerto `GeneradorExcelPort`, implementado en `infrastructure/adapter/export/ExcelWorkbookBuilder.java` con Apache POI. Así la capa de aplicación no depende de POI.

```java
package co.edu.unimagdalena.avicontrol.application.service;

@Service
public class ExportarExcelService implements ExportarExcelUseCase {

    private final ConsultarDesgloseUseCase consultarDesglose;
    private final GeneradorExcelPort generadorExcel;

    // Constructor...

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteExcelLiquidacion(Long idLiquidacion) {
        var desglose = consultarDesglose.consultarDesglose(idLiquidacion);   // 404 si no existe
        if (!desglose.exportacionPermitida()) {
            throw new ExportacionNoPermitidaException(idLiquidacion);         // 409 (CU05.FR-004)
        }
        return generadorExcel.generar(desglose);                              // mismos valores que la pantalla (CA-G06)
    }
}
```

```java
package co.edu.unimagdalena.avicontrol.infrastructure.adapter.export;

@Component
public class ExcelWorkbookBuilder implements GeneradorExcelPort {

    @Override
    public byte[] generar(DesgloseDto desglose) {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Liquidacion_Lote");
            var copStyle = workbook.createCellStyle();
            copStyle.setDataFormat(workbook.createDataFormat().getFormat("$#,##0"));

            int fila = escribirMatrizVentaFinal(sheet, desglose, copStyle, 0);
            int primeraPartida = fila + 2;
            int ultimaPartida = escribirPartidas(sheet, desglose.categorias(), copStyle, primeraPartida);

            var total = sheet.createRow(ultimaPartida + 1);
            total.createCell(0).setCellValue("TOTAL COSTOS OPERATIVOS");
            var totalCell = total.createCell(5);
            totalCell.setCellFormula("SUM(F%d:F%d)".formatted(primeraPartida + 1, ultimaPartida));
            totalCell.setCellStyle(copStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ExportacionFallidaException(desglose.idLiquidacion(), e);  // 500 error-interno
        }
    }
}
```

### Adaptador REST con Cabeceras `.xlsx`: `DesgloseController.java`
```java
package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

@RestController
@RequestMapping("/api/v1/liquidaciones")
public class DesgloseController {

    private final ConsultarDesgloseUseCase consultarDesgloseUseCase;
    private final ExportarExcelUseCase exportarExcelUseCase;

    // Constructor...

    @GetMapping("/{id}/desglose")
    public ResponseEntity<DesgloseDto> consultarDesglose(@PathVariable Long id) {
        return ResponseEntity.ok(consultarDesgloseUseCase.consultarDesglose(id));
    }

    @GetMapping("/{id}/desglose/excel")
    public ResponseEntity<byte[]> descargarExcelDesglose(@PathVariable Long id) {
        byte[] bytes = exportarExcelUseCase.generarReporteExcelLiquidacion(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Liquidacion_Lote_" + id + ".xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }
}
```

### Adaptador REST del Historial: `HistorialController.java`
```java
package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

@Validated
@RestController
@RequestMapping("/api/v1/liquidaciones")
public class HistorialController {

    private final ConsultarHistorialUseCase consultarHistorialUseCase;

    // Constructor...

    /** GET /api/v1/liquidaciones?search=norte&desde=2026-07-01&hasta=2026-09-30&page=0&size=10 */
    @GetMapping
    public ResponseEntity<HistorialPageDto> consultarHistorial(
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(consultarHistorialUseCase.consultarHistorial(search, desde, hasta, page, size));
    }
}
```

---

## Phase 1: CU06 – Historial Cronológico de Liquidaciones

- [ ] **T001** Crear puerto `ConsultarHistorialUseCase.java` en `domain/port/in/` y DTOs `HistorialItemDto.java` y `HistorialPageDto.java` en `domain/port/in/dto/`.
- [ ] **T002** Unit Test `ConsultarHistorialServiceTest.java`: búsqueda por nombre y UUID de galpón y de lote, rango de fechas inclusivo en zona `America/Bogota`, `desde > hasta` rechazado, orden descendente, inclusión de `ANULADA` y los dos mensajes de FR-007.
- [ ] **T003** Implementar `ConsultarHistorialService.java`.
- [ ] **T004** Integration Test e implementación de `GET /api/v1/liquidaciones` en `HistorialController.java` (query params `search`, `desde`, `hasta`, `page`, `size`; `400` por rango o parámetros inválidos); verificar que cada fila incluye el `idLiquidacion` que sirve de enlace a la vista de Liquidación (M3-CU03.FR-014), no al desglose directamente.

---

## Phase 2: CU05 – Consultar Desglose de Ventas y Gastos

- [ ] **T005** Crear puerto `ConsultarDesgloseUseCase.java` y los DTOs del desglose en `domain/port/in/dto/`.
- [ ] **T006** Unit Test `ConsultarDesgloseServiceTest.java`: agrupación por las tres categorías, suma igual a `costosOperativosCop`, sección de ingresos, siniestro total, Liquidación `ANULADA` con registro de anulación y exportación deshabilitada.
- [ ] **T007** Implementar `ConsultarDesgloseService.java`.
- [ ] **T008** Integration Test e implementación de `GET /api/v1/liquidaciones/{id}/desglose` en `DesgloseController.java`.

---

## Phase 3: CU05 – Generación y Exportación de Reporte Excel (Apache POI)

- [ ] **T009** Unit Test `ExportarExcelServiceTest.java` (rechazo de `ANULADA`) y `ExcelWorkbookBuilderTest.java` (valores idénticos a la pantalla, fórmula `SUM` sobre el rango real, formato `$#,##0`).
- [ ] **T010** Implementar `ExportarExcelService.java`, el puerto `GeneradorExcelPort` y `ExcelWorkbookBuilder.java`.
- [ ] **T011** Integration Test e implementación de `GET /api/v1/liquidaciones/{id}/desglose/excel` (`200`, `404`, `409`).
