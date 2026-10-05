# Implementation Plan: Historial de Liquidaciones y Exportación de Desglose

**Date**: 2026-09-28  
**Actualizado**: 2026-10-05  
**Specs**:
- [m3-cu05-desglose-ventas-gastos](../features/m3-cu05-desglose-ventas-gastos/spec.md) – Consultar Desglose de Ventas y Gastos  
- [m3-cu06-historial-liquidaciones](../features/m3-cu06-historial-liquidaciones/spec.md) – Consultar Historial de Liquidaciones  

## Summary

Este plan aborda la **consulta histórica cronológica** de liquidaciones (`CU06`) y la **visualización y exportación en Excel del desglose pormenorizado de ventas y gastos** (`CU05`).

**Historial (CU06)**: Permite al Administrador Financiero buscar liquidaciones por nombre o UUID de galpón o de lote y filtrarlas por rango de fechas (orden descendente). Es el **único punto de acceso** a las Liquidaciones ya generadas, porque la lista de lotes no muestra los lotes con Liquidación `ACTIVA` (M3-CU01.FR-014). Al seleccionar cualquier fila, el sistema abre la **vista de la Liquidación** (M3-CU03.FR-014) — no el desglose directamente. El Desglose se consulta desde la propia vista de la Liquidación (CU06.FR-006).

**Desglose (CU05)**: Visualización agrupada por categorías de costo — **Alimento**, **Insumos Médicos** y **Costo de Población** (renombrada desde "Población Inicial" para alinear con CU03.FR-003 y CU07) — garantizando que los subtotales cuadran exactamente con `costosOperativosCop` de la Liquidación (FR-002). Exportación a `.xlsx` con Apache POI (`poi-ooxml:5.2.5`).

## Technical Context

- **Language/Version**: Java 21 (LTS)
- **Primary Dependencies**: Spring Boot 3.3.x, Spring Data JPA, Spring Web MVC, Apache POI `poi-ooxml:5.2.5`, JUnit 5, MockMvc
- **Storage**: Proyección y lectura sobre tablas `liquidacion`, `partida_costo_lote`, `registro_anulacion`
- **Testing**: Unit tests de lógica de proyección/agrupación, tests de generación de Excel con Apache POI y tests REST
- **Performance Goals**: Generación y descarga de archivo Excel < 10s (SC-002); consulta de historial < 3s (SC-001)

---

## Contrato REST del Historial (CU06)

### `GET /api/v1/liquidaciones`

| Query param | Tipo | Obligatorio | Valor por defecto | Regla |
|---|---|---|---|---|
| `search` | string | No | — | Texto libre, sin distinguir mayúsculas: coincide por contenido con nombre de galpón o de lote, o exacto con UUID de galpón o de lote (FR-003). Omitido = todas. Máx. 100 caracteres |
| `desde` | fecha ISO `yyyy-MM-dd` | No | — | Incluye las Liquidaciones generadas desde las 00:00 de ese día (hora Colombia) |
| `hasta` | fecha ISO `yyyy-MM-dd` | No | — | Incluye hasta las 23:59:59 de ese día. Si `desde > hasta` → `400` `rango-fechas-invalido` (FR-005) |
| `page` | int | No | `0` | Índice 0-based, `>= 0` |
| `size` | int | No | `10` | `1..50` |

- Orden fijo: `fechaHoraLiquidacion` descendente (FR-001). No se expone `sort`.
- Incluye Liquidaciones `ACTIVA` y `ANULADA` (FR-003).
- Respuestas: `200` (incluso con `content` vacío), `400` (rango o parámetros inválidos), `401`/`403`. Ver el catálogo de errores en la arquitectura general.

```json
GET /api/v1/liquidaciones?search=norte&desde=2026-07-01&hasta=2026-09-30&page=0&size=10

HTTP/1.1 200 OK
{
  "content": [
    {
      "idLiquidacion": 42,
      "fechaHoraLiquidacion": "2026-09-28T16:42:00Z",
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
      "fechaHoraLiquidacion": "2026-09-20T09:15:00Z",
      "idGalpon": "f1e2d3c4-e89b-12d3-a456-426614174000",
      "nombreGalpon": "Galpón Norte",
      "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
      "nombreLote": "Lote Norte Ciclo 4",
      "ventaBrutaCop": 127600000,
      "porcentajeMortalidad": 4.00,
      "costosOperativosCop": 60200000,
      "utilidadNetaCop": 67400000,
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

## Snippets de Código Java de Puertos, DTOs, Servicio Apache POI y Controlador

### Puertos de Entrada
```java
package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialPageDto;
import java.time.LocalDate;
import java.util.UUID;

/** CU06 — Consultar historial cronológico filtrado */
public interface ConsultarHistorialUseCase {
    /**
     * @param search texto libre: nombre o UUID de galpón o de lote (null = todas)
     * @param desde  fecha inicial inclusiva (null = sin límite)
     * @param hasta  fecha final inclusiva (null = sin límite); desde > hasta → excepción de rango inválido
     */
    HistorialPageDto consultarHistorial(String search, LocalDate desde, LocalDate hasta, int page, int size);
}

/** CU05 — Consultar proyección de desglose de ventas y gastos */
public interface ConsultarDesgloseUseCase {
    DesgloseDto consultarDesglose(Long idLiquidacion);
}

/** CU05 — Generar y descargar reporte Excel (.xlsx) */
public interface ExportarExcelUseCase {
    byte[] generarReporteExcelLiquidacion(Long idLiquidacion);
}
```

### DTOs de Respuesta: `HistorialPageDto.java` y `DesgloseDto.java`
```java
package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HistorialItemDto(
    Long idLiquidacion,
    Instant fechaHoraLiquidacion,
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

public record PartidaDesgloseDto(
    String concepto,
    BigDecimal cantidad,
    String unidadMedida,
    BigDecimal precioUnitarioCop,
    long subtotalCop,
    String referenciaOrigen
) {}

public record DesgloseDto(
    Long idLiquidacion,
    String estado,
    boolean esSiniestroTotal,
    List<PartidaDesgloseDto> partidasAlimento,
    List<PartidaDesgloseDto> partidasInsumosMedicos,
    List<PartidaDesgloseDto> partidasCostoPoblacion,
    long totalCostosOperativosCop,
    long ventaBrutaCop,
    long utilidadNetaCop
) {}
```

### Servicio Apache POI: `ExportarExcelService.java`
```java
package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.port.in.ExportarExcelUseCase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class ExportarExcelService implements ExportarExcelUseCase {

    @Override
    public byte[] generarReporteExcelLiquidacion(Long idLiquidacion) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Liquidacion_Lote");

            // Estilo de celda para moneda COP ($#,##0)
            CellStyle copStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            copStyle.setDataFormat(format.getFormat("$#,##0"));

            // Encabezados y Matriz de Venta
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Concepto");
            headerRow.createCell(1).setCellValue("Subtotal (COP)");

            // Partidas y fórmula de suma nativa Excel
            Row totalRow = sheet.createRow(10);
            totalRow.createCell(0).setCellValue("TOTAL COSTOS OPERATIVOS");
            Cell totalCell = totalRow.createCell(1);
            totalCell.setCellFormula("SUM(B2:B9)");
            totalCell.setCellStyle(copStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte Excel POI", e);
        }
    }
}
```

### Adaptador REST con Cabeceras `.xlsx`: `DesgloseController.java`
```java
package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarDesgloseUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.ExportarExcelUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/liquidaciones")
public class DesgloseController {

    private final ConsultarDesgloseUseCase consultarDesgloseUseCase;
    private final ExportarExcelUseCase exportarExcelUseCase;

    public DesgloseController(ConsultarDesgloseUseCase consultarDesgloseUseCase,
                              ExportarExcelUseCase exportarExcelUseCase) {
        this.consultarDesgloseUseCase = consultarDesgloseUseCase;
        this.exportarExcelUseCase = exportarExcelUseCase;
    }

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

import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarHistorialUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialPageDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/liquidaciones")
public class HistorialController {

    private final ConsultarHistorialUseCase consultarHistorialUseCase;

    public HistorialController(ConsultarHistorialUseCase consultarHistorialUseCase) {
        this.consultarHistorialUseCase = consultarHistorialUseCase;
    }

    /**
     * GET /api/v1/liquidaciones?search=norte&desde=2026-07-01&hasta=2026-09-30&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<HistorialPageDto> consultarHistorial(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
            consultarHistorialUseCase.consultarHistorial(search, desde, hasta, page, size));
    }
}
```

---

## Phase 1: CU06 – Historial Cronológico de Liquidaciones

- [ ] **T001** Crear puerto `ConsultarHistorialUseCase.java` en `domain/port/in/` y DTOs `HistorialItemDto.java` y `HistorialPageDto.java` en `domain/port/in/dto/`.
- [ ] **T002** Unit Test `ConsultarHistorialServiceTest.java`: búsqueda por nombre y UUID de galpón y de lote, rango de fechas inclusivo, `desde > hasta` rechazado, orden descendente, inclusión de `ANULADA` y los dos mensajes de FR-007.
- [ ] **T003** Implementar `ConsultarHistorialService.java`.
- [ ] **T004** Integration Test e implementación de `GET /api/v1/liquidaciones` en `HistorialController.java` (query params `search`, `desde`, `hasta`, `page`, `size`; `400` por rango inválido); verificar que cada fila incluye el `idLiquidacion` que sirve de enlace a la vista de Liquidación (M3-CU03.FR-014), no al desglose directamente.

---

## Phase 2: CU05 – Consultar Desglose de Ventas y Gastos

- [ ] **T005** Crear puerto `ConsultarDesgloseUseCase.java` en `domain/port/in/` y DTO `DesgloseDto.java` en `domain/port/in/dto/`.
- [ ] **T006** Unit Test `ConsultarDesgloseServiceTest.java`.
- [ ] **T007** Implementar `ConsultarDesgloseService.java`.
- [ ] **T008** Integration Test e implementación de `GET /api/v1/liquidaciones/{id}/desglose` en `DesgloseController.java`.

---

## Phase 3: CU05 – Generación y Exportación de Reporte Excel (Apache POI)

- [ ] **T009** Unit Test `ExportarExcelServiceTest.java`.
- [ ] **T010** Implementar `ExportarExcelService.java`.
- [ ] **T011** Integration Test e implementación de `GET /api/v1/liquidaciones/{id}/desglose/excel`.
