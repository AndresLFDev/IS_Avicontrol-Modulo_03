# Implementation Plan: Consultar Lista de Lotes

**Date**: 2026-09-28  
**Actualizado**: 2026-10-23  
**Arquitectura y catálogo de errores**: [general.md](../arquitectura/general.md)  
**Specs**:
- [m3-cu01-lista-lotes](../features/m3-cu01-lista-lotes/spec.md) – Consultar Lista de Lotes  

## Summary

Permite al Administrador Financiero consultar la **lista de lotes sin Liquidación `ACTIVA`** alojados en galpones, alimentada exclusivamente desde la copia local sincronizada de M3-CU07. Cada fila muestra UUID y nombre del lote, fecha de ingreso, nombre y UUID del galpón y la **etapa** derivada (FR-013). La página informa además la fecha y hora de la última sincronización exitosa con Módulo 1 y con Módulo 2 (FR-001, RT-09). **No se muestra columna de mortalidad** en la lista (la mortalidad se presenta en la Liquidación).

Cuando la población actual sincronizada del lote sea 0 por mortalidad total, la fila muestra el distintivo **"Siniestro total"** junto a su etapa (FR-001). Cada fila expone **un único botón** determinado por la etapa:
- `Por Liquidar` → "Generar liquidación" (habilitado si hay resultado de sacrificio válido o siniestro total confirmado).
- `Productivo`, `En Cosecha`, `Aislamiento` → "Generar liquidación" (deshabilitado).

Los lotes en etapa `Liquidado` **no se listan** (M3-CU01.FR-014): se consultan desde el historial (plan 004). Si su Liquidación se anula, vuelven a la lista como `Por Liquidar`. La lista no ofrece "Ver liquidación" (FR-004).

Incluye filtrado por etapa (una etapa o "Todos"), búsqueda por nombre de lote, nombre de galpón o UUID, paginación a máximo 6 lotes por página e indicador de progreso (ej. "6 de 7 lotes").

## Technical Context

- **Language/Version**: Java 21 (LTS)
- **Primary Dependencies**: Spring Boot 4.1.x, Spring Data JPA, Spring Web MVC, Jakarta Validation, JUnit 5, MockMvc
- **Storage**: Consulta sobre la copia local (`galpon`, `lote`, `alerta_vaciado_sanitario`, `resultado_final_sacrificio`) y sobre `liquidacion`
- **Testing**: Unit tests con Mockito y pruebas de integración REST con MockMvc + Testcontainers (PostgreSQL)
- **Target Platform**: JVM 21 / REST API
- **Project Type**: Backend REST API (caso de uso de consulta)
- **Performance Goals**: Tiempo de respuesta HTTP GET < 3s (SC-001)

---

## Etapas y Regla de Derivación (FR-013)

La **etapa** del lote es un valor derivado por M3 (nunca almacenado ni comunicado a M1), calculada aplicando las siguientes reglas en orden; la primera que se cumple determina la etapa:

| Prioridad | Condición | Etapa derivada |
|---|---|---|
| 1 | Existe alerta de vaciado sanitario **y** Liquidación `ACTIVA` del lote | `LIQUIDADO` (**se excluye de la lista**, FR-014) |
| 2 | Existe alerta de vaciado sanitario **y** no existe Liquidación `ACTIVA` | `POR_LIQUIDAR` |
| 3 | Lote vinculado a galpón en estado `PRODUCTIVO` | `PRODUCTIVO` |
| 4 | Lote vinculado a galpón en estado `EN_COSECHA` | `EN_COSECHA` |
| 5 | Lote vinculado a galpón en estado `AISLAMIENTO` | `AISLAMIENTO` |

Un galpón sin lote vinculado ni alerta pendiente no genera fila. Un lote sin alerta cuyo galpón está en otro estado (`DISPONIBLE`, `MANTENIMIENTO`, `VACIADO_SANITARIO` reocupado) tampoco genera fila.

La derivación se resuelve **en la consulta SQL** (un `CASE` sobre `LEFT JOIN` a la alerta, a la Liquidación `ACTIVA` y al galpón), de modo que el filtro por etapa, la exclusión de `LIQUIDADO` y el total de la paginación se calculan en la base de datos, sin consultas adicionales por fila.

---

## Contrato REST

### `GET /api/v1/lotes`

| Query param | Tipo | Obligatorio | Valor por defecto | Regla |
|---|---|---|---|---|
| `etapa` | enum `EtapaLote` | No | (todas) | Una de `PRODUCTIVO`, `EN_COSECHA`, `AISLAMIENTO`, `POR_LIQUIDAR`. Omitido = "Todos". Cualquier otro valor (incluido `LIQUIDADO`) → `400` |
| `search` | string | No | — | Texto libre, sin distinguir mayúsculas: coincide por contenido con nombre de lote o nombre de galpón, o exacto con UUID de lote o galpón. Máx. 100 caracteres |
| `page` | int | No | `0` | Índice 0-based, `>= 0`; si no, `400` |
| `size` | int | No | `6` | `>= 1` (si no, `400`); un valor mayor que 6 se limita a 6 (FR-011) |

### Catálogo de etapas (`EtapaLote`)

El API usa el **código**; la etiqueta es el texto que muestra el frontend.

| Código (API) | Etiqueta (UI) | ¿Se lista? | Acción de la fila |
|---|---|---|---|
| `PRODUCTIVO` | Productivo | Sí | `GENERAR_LIQUIDACION` deshabilitada |
| `EN_COSECHA` | En Cosecha | Sí | `GENERAR_LIQUIDACION` deshabilitada |
| `AISLAMIENTO` | Aislamiento | Sí | `GENERAR_LIQUIDACION` deshabilitada |
| `POR_LIQUIDAR` | Por Liquidar | Sí | `GENERAR_LIQUIDACION` habilitada si hay resultado de sacrificio o siniestro total |
| `LIQUIDADO` | Liquidado | **No** (FR-014) | — (se consulta en el historial) |

### Respuestas

| Código | Cuándo |
|---|---|
| `200 OK` | Siempre que los parámetros sean válidos, incluso con `content` vacío (vacío total o sin resultados) |
| `400 Bad Request` | `etapa` fuera del catálogo listable, `page < 0`, `size < 1` o `search` de más de 100 caracteres (`ProblemDetail` `validacion`, ver arquitectura general) |

### Mensajes

| Situación | `mensaje` |
|---|---|
| Hay resultados | `"<mostrados> de <total> lotes"` (FR-012), p. ej. `"6 de 7 lotes"` |
| No hay ningún lote listable y no se envió `etapa` ni `search` | `"No hay lotes disponibles para consultar"` (FR-006) |
| Hay filtro o búsqueda y no hay coincidencias, o la página pedida está fuera de rango | `"No se encontraron resultados"` (FR-010) |

`totalElements` y `totalPages` siempre reflejan el total real del filtro aplicado, aunque la página pedida esté vacía.

---

## JSON Payload de Ejemplo (Respuesta Paginada)

```json
GET /api/v1/lotes?etapa=POR_LIQUIDAR&search=norte&page=0&size=6

HTTP/1.1 200 OK
{
  "content": [
    {
      "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
      "nombreLote": "Lote Norte Ciclo 4",
      "fechaIngreso": "2026-06-01",
      "idGalpon": "f1e2d3c4-e89b-12d3-a456-426614174000",
      "nombreGalpon": "Galpón Norte",
      "etapa": "POR_LIQUIDAR",
      "siniestroTotal": false,
      "accion": "GENERAR_LIQUIDACION",
      "accionHabilitada": true
    },
    {
      "idLote": "b2c3d4e5-e89b-12d3-a456-426614174000",
      "nombreLote": "Lote Sur Ciclo 3",
      "fechaIngreso": "2026-05-15",
      "idGalpon": "e2d3c4b5-e89b-12d3-a456-426614174000",
      "nombreGalpon": "Galpón Sur",
      "etapa": "POR_LIQUIDAR",
      "siniestroTotal": true,
      "accion": "GENERAR_LIQUIDACION",
      "accionHabilitada": true
    }
  ],
  "page": 0,
  "size": 6,
  "totalElements": 2,
  "totalPages": 1,
  "mensaje": "2 de 2 lotes",
  "sincronizacion": {
    "modulo1": { "ultimaSincronizacionExitosa": "2026-10-03T10:00:00Z", "desactualizada": false },
    "modulo2": { "ultimaSincronizacionExitosa": "2026-10-03T09:50:00Z", "desactualizada": false }
  }
}
```

---

## Snippets de Código Java de Puertos, DTOs y Servicio

### Puerto de Entrada: `ListarLotesUseCase.java`
```java
package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LotePageDto;

public interface ListarLotesUseCase {
    /**
     * @param etapa  etapa listable (PRODUCTIVO, EN_COSECHA, AISLAMIENTO, POR_LIQUIDAR) o null para todas;
     *               LIQUIDADO se rechaza con error de validación
     * @param search texto libre: nombre de lote, nombre de galpón o UUID
     * @param page   índice de página (0-based)
     * @param size   tamaño de página (se limita a 6 en el servicio)
     */
    LotePageDto listarLotes(EtapaLote etapa, String search, int page, int size);
}
```

### Puerto de Salida: `LoteRepository.java`
```java
package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.model.LoteListado;
import co.edu.unimagdalena.avicontrol.domain.shared.Pagina;

public interface LoteRepository {
    /**
     * Devuelve los lotes con su etapa ya derivada (FR-013), filtrando por etapa (o todas si es null)
     * y buscando por nombre de lote, de galpón o UUID. Excluye siempre los lotes LIQUIDADO
     * (M3-CU01.FR-014), también del total de la paginación.
     */
    Pagina<LoteListado> buscarListables(EtapaLote etapa, String search, int page, int size);

    /** Indica si existe al menos un lote listable, sin filtros (para el mensaje de FR-006). */
    boolean existeAlgunoListable();
}
```

`LoteListado` es un `record` del dominio con `idLote`, `nombreLote`, `fechaIngreso`, `idGalpon`, `nombreGalpon`, `etapa`, `poblacionActual` y `tieneResultadoSacrificio`. `Pagina<T>` (`domain/shared`) es un `record(List<T> contenido, int page, int size, long totalElements, int totalPages)` de Java puro.

### DTOs de Respuesta: `LoteResumenDto.java` y `LotePageDto.java`
```java
package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Proyección de solo lectura por fila de la lista de lotes (M3-CU01).
 * No incluye mortalidad (la mortalidad se muestra en la Liquidación).
 */
public record LoteResumenDto(
    UUID idLote,
    String nombreLote,
    LocalDate fechaIngreso,
    UUID idGalpon,
    String nombreGalpon,
    String etapa,              // "PRODUCTIVO" | "EN_COSECHA" | "AISLAMIENTO" | "POR_LIQUIDAR"
    boolean siniestroTotal,    // true cuando poblacionActual == 0 por mortalidad total
    String accion,             // siempre "GENERAR_LIQUIDACION" (FR-005)
    boolean accionHabilitada   // false cuando la etapa no permite la acción
) {}

public record LotePageDto(
    List<LoteResumenDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    String mensaje,                         // ver tabla de mensajes
    EstadoSincronizacionDto sincronizacion  // última sincronización exitosa por fuente (RT-09)
) {}
```

### Servicio de Aplicación: `ListarLotesService.java`
```java
package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.exception.ParametroInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.model.LoteListado;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarEstadoSincronizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.ListarLotesUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LotePageDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LoteResumenDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarLotesService implements ListarLotesUseCase {

    private static final int TAMANO_MAXIMO = 6; // FR-011

    private final LoteRepository loteRepository;
    private final ConsultarEstadoSincronizacionUseCase estadoSincronizacion;

    public ListarLotesService(LoteRepository loteRepository, ConsultarEstadoSincronizacionUseCase estadoSincronizacion) {
        this.loteRepository = loteRepository;
        this.estadoSincronizacion = estadoSincronizacion;
    }

    @Override
    @Transactional(readOnly = true)
    public LotePageDto listarLotes(EtapaLote etapa, String search, int page, int size) {
        if (etapa == EtapaLote.LIQUIDADO) {
            throw new ParametroInvalidoException("etapa", "La etapa LIQUIDADO no se lista; consulte el historial.");
        }
        int pageSize = Math.min(size, TAMANO_MAXIMO);
        var resultado = loteRepository.buscarListables(etapa, search, page, pageSize);
        var filas = resultado.contenido().stream().map(this::toDto).toList();

        String mensaje;
        if (!filas.isEmpty()) {
            mensaje = filas.size() + " de " + resultado.totalElements() + " lotes";          // FR-012
        } else if (etapa == null && (search == null || search.isBlank()) && !loteRepository.existeAlgunoListable()) {
            mensaje = "No hay lotes disponibles para consultar";                             // FR-006
        } else {
            mensaje = "No se encontraron resultados";                                        // FR-010
        }
        return new LotePageDto(filas, page, pageSize, resultado.totalElements(), resultado.totalPages(),
                mensaje, estadoSincronizacion.consultar());
    }

    private LoteResumenDto toDto(LoteListado lote) {
        boolean siniestro = lote.poblacionActual() == 0;
        // FR-005: acción única; solo se habilita en POR_LIQUIDAR con resultado de sacrificio o siniestro total
        boolean habilitada = lote.etapa() == EtapaLote.POR_LIQUIDAR && (lote.tieneResultadoSacrificio() || siniestro);
        return new LoteResumenDto(lote.idLote(), lote.nombreLote(), lote.fechaIngreso(),
                lote.idGalpon(), lote.nombreGalpon(), lote.etapa().name(), siniestro,
                "GENERAR_LIQUIDACION", habilitada);
    }
}
```

### Controlador REST: `LoteController.java`
```java
package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.port.in.ListarLotesUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LotePageDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/lotes")
public class LoteController {

    private final ListarLotesUseCase listarLotesUseCase;

    public LoteController(ListarLotesUseCase listarLotesUseCase) {
        this.listarLotesUseCase = listarLotesUseCase;
    }

    /** GET /api/v1/lotes?etapa=POR_LIQUIDAR&search=norte&page=0&size=6 */
    @GetMapping
    public ResponseEntity<LotePageDto> listarLotes(
            @RequestParam(required = false) EtapaLote etapa,                 // valor desconocido → 400
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "6") @Min(1) int size) {
        return ResponseEntity.ok(listarLotesUseCase.listarLotes(etapa, search, page, size));
    }
}
```

---

## Phase 1: Puertos y DTOs

- [ ] **T001** Crear el enum `EtapaLote`, el record `LoteListado` y `Pagina<T>` en el dominio.
- [ ] **T002** Crear puerto `ListarLotesUseCase.java` en `domain/port/in/` y DTOs `LoteResumenDto.java` y `LotePageDto.java` en `domain/port/in/dto/`.

---

## Phase 2: Lógica de Servicio — Etapa, Acción Única y Mensajes

- [ ] **T003** Unit Test `ListarLotesServiceTest.java`: rechazo de `LIQUIDADO`, distintivo `siniestroTotal`, botón habilitado/deshabilitado, límite de 6 ítems, los tres mensajes (incluida página fuera de rango) y mensaje de paginación (ej. "6 de 7 lotes").
- [ ] **T004** Implementar `ListarLotesService.java`.

---

## Phase 3: Persistencia — Consulta con Etapa Derivada

- [ ] **T005** Integration Test `LoteRepositoryAdapterIT.java` (Testcontainers): derivación de etapa (FR-013) para los 5 casos, exclusión de lotes con Liquidación `ACTIVA` y su reaparición como `POR_LIQUIDAR` tras la anulación (FR-014), galpón reocupado, filtro, búsqueda por nombre y UUID, total de paginación.
- [ ] **T006** Implementar `buscarListables` y `existeAlgunoListable` en el adaptador de persistencia.

---

## Phase 4: Adaptador REST (Controlador HTTP)

- [ ] **T007** Controller Test `LoteControllerTest.java` con MockMvc: parámetros de query (incluido `400` para `etapa` inválida o `LIQUIDADO`, `page < 0`, `size < 1`, `search` > 100), respuesta paginada y formato JSON.
- [ ] **T008** Implementar `LoteController.java`.
