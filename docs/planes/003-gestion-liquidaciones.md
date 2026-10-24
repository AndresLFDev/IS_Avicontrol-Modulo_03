# Implementation Plan: Gestión del Ciclo de Vida de Liquidaciones

**Date**: 2026-09-28  
**Actualizado**: 2026-10-23  
**Arquitectura, DDL y catálogo de errores**: [general.md](../arquitectura/general.md)  
**Specs**:
- [m3-cu03-generar-liquidacion](../features/m3-cu03-generar-liquidacion/spec.md) – Generar Liquidación del Lote  
- [m3-cu04-anular-liquidacion](../features/m3-cu04-anular-liquidacion/spec.md) – Anular Liquidación  

## Summary

Este plan cubre el núcleo de dominio del Módulo 3: la **generación inmutable de la Liquidación** de un lote (`CU03`) y su **anulación auditable** (`CU04`).

**Generación en dos pasos (CU03.FR-015)**: El usuario ingresa primero el precio por kg y su nombre como usuario responsable (paso 1 — captura); el sistema genera una **vista previa no persistida** que muestra la Matriz de Venta Final con todos los indicadores (paso 2 — vista previa); el usuario confirma "Generar liquidación" para persistir. "Volver" regresa sin crear ningún registro. La vista previa se identifica con el aviso "Vista previa · sin generar" — que **no es un estado** de la Liquidación. La vista previa devuelve una `huellaDatos` que la confirmación reenvía para garantizar que se persiste exactamente lo mostrado.

**Consulta de Liquidación existente (CU03.FR-014)**: Desde el historial (plan 004) o justo después de generarla, se abre la vista de la Liquidación (`ACTIVA` o `ANULADA`) con la Matriz de Venta Final, subtotales por categoría (Alimento, Insumos Médicos, Costo de Población), fecha de sincronización de cada fuente, el botón para iniciar la anulación si está `ACTIVA` (CU04.FR-002b) y el registro de anulación si está `ANULADA`. La lista de lotes no ofrece acceso a Liquidaciones (M3-CU01.FR-004).

**Anulación (CU04)**: Se inicia exclusivamente desde la vista de una Liquidación `ACTIVA`. Requiere motivo justificado (10-500 caracteres), el nombre del usuario responsable y confirmación explícita, y opera en transacción atómica. Preserva el registro histórico sin borrar filas y rehabilita el lote para una nueva liquidación.

Calcula Venta Bruta, Mortalidad del Lote, Costos Operativos y Utilidad Neta con redondeo `HALF_UP` a enteros COP y 2 decimales para porcentajes. Bloquea duplicados activos (índice único parcial `uq_lote_activa`).

## Technical Context

- **Language/Version**: Java 21 (LTS)
- **Primary Dependencies**: Spring Boot 4.1.x, Spring Data JPA, Spring Web MVC, Jakarta Validation, Flyway (`V3__create_liquidacion_tables.sql`, DDL en la arquitectura general), JUnit 5, MockMvc
- **Storage**: PostgreSQL (`liquidacion`, `partida_costo_lote`, `snapshot_datos_origen`, `registro_anulacion`, `aviso_utilizacion_resultado`)
- **Testing**: Unit tests exhaustivos del cálculo financiero y pruebas de integración REST con Testcontainers
- **Target Platform**: JVM 21 / REST API

---

## Precondiciones de la Generación (CU03.FR-005, FR-006, FR-007, FR-009)

Se evalúan en este orden, en la vista previa **y** en la confirmación:

| # | Validación | Error |
|---|---|---|
| 1 | Formato del body (Bean Validation en el Request): `usuario` de 3 a 100 caracteres; `precioKgCop`, si viene, `> 0` | `400 validacion` |
| 2 | El lote existe en la copia local | `404 lote-no-encontrado` |
| 3 | Precio coherente con el lote: obligatorio si no es siniestro total, ausente si lo es | `400 validacion` |
| 4 | El lote no tiene Liquidación `ACTIVA` | `409 liquidacion-ya-existente` (con `idLiquidacionActiva`) |
| 5 | Existe alerta de vaciado sanitario del lote | `422` `causa = SIN_ALERTA_VACIADO` |
| 6 | Existe resultado de sacrificio válido, **o** `poblacion_actual = 0` (siniestro total) | `422` `causa = SIN_RESULTADO_SACRIFICIO` |
| 7 | Existe al menos una partida de alimento | `422` `causa = SIN_PARTIDAS_ALIMENTO` |
| 8 | Ninguna partida de alimento ni tramo de medicamento está `PENDIENTE_PRECIO` | `422` `causa = PARTIDAS_SIN_PRECIO` (con `partidasPendientes`) |
| 9 | Solo en la confirmación: `huellaDatos` coincide con la recalculada | `409 vista-previa-desactualizada` |

La validación 3 va después del `404` porque depende del lote: solo con su `poblacion_actual` se sabe si es siniestro total.

**Siniestro total** = `lote.poblacion_actual = 0` (no "ausencia de resultado"). Un lote sin resultado de sacrificio y con aves vivas no es siniestro: falla en la validación 6.

---

## JSON Payloads

### Paso 1 — Captura del Precio (Request)

```json
POST /api/v1/liquidaciones/previa

{
  "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
  "precioKgCop": 5800.00,
  "usuario": "Juan Grau"
}
```

El galpón no se envía: se toma de la alerta de vaciado sanitario del lote.

### Paso 2 — Vista Previa (Response — NO se persiste)

```json
HTTP/1.1 200 OK
{
  "esVistaPrevia": true,
  "aviso": "Vista previa · sin generar",
  "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
  "idGalpon": "f1e2d3c4-e89b-12d3-a456-426614174000",
  "nombreLote": "Lote Norte Ciclo 4",
  "nombreGalpon": "Galpón Norte",
  "siniestroTotal": false,
  "precioKgCop": 5800.00,
  "pollosVendidos": 9600,
  "pesoTotalKg": 24000.00,
  "pesoPromedioKg": 2.50,
  "ventaBrutaCop": 139200000,
  "mortalidadAves": 400,
  "porcentajeMortalidad": 4.00,
  "subtotalesCategoria": {
    "alimento": 45000000,
    "insumosMedicos": 3200000,
    "costoPoblacion": 12000000
  },
  "costosOperativosCop": 60200000,
  "utilidadNetaCop": 79000000,
  "fechaSincronizacionM1": "2026-10-01T08:00:00Z",
  "fechaSincronizacionM2": "2026-10-02T14:30:00Z",
  "huellaDatos": "9f2c1e7a4b0d3c58e6a1f7b2d4c9e0a35b8f6d1c2e7a9b4f0d3c6e8a1b5f7d2c"
}
```

### Confirmación — Generar Liquidación

```json
POST /api/v1/liquidaciones

{
  "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
  "precioKgCop": 5800.00,
  "usuario": "Juan Grau",
  "huellaDatos": "9f2c1e7a4b0d3c58e6a1f7b2d4c9e0a35b8f6d1c2e7a9b4f0d3c6e8a1b5f7d2c"
}

HTTP/1.1 201 Created
Location: /api/v1/liquidaciones/42
{
  "idLiquidacion": 42,
  "estado": "ACTIVA",
  "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000",
  "idGalpon": "f1e2d3c4-e89b-12d3-a456-426614174000",
  "nombreLote": "Lote Norte Ciclo 4",
  "nombreGalpon": "Galpón Norte",
  "siniestroTotal": false,
  "precioKgCop": 5800.00,
  "pollosVendidos": 9600,
  "pesoTotalKg": 24000.00,
  "pesoPromedioKg": 2.50,
  "ventaBrutaCop": 139200000,
  "mortalidadAves": 400,
  "porcentajeMortalidad": 4.00,
  "subtotalesCategoria": {
    "alimento": 45000000,
    "insumosMedicos": 3200000,
    "costoPoblacion": 12000000
  },
  "costosOperativosCop": 60200000,
  "utilidadNetaCop": 79000000,
  "fechaHoraGeneracion": "2026-10-03T11:00:00Z",
  "usuarioResponsable": "Juan Grau",
  "fechaSincronizacionM1": "2026-10-01T08:00:00Z",
  "fechaSincronizacionM2": "2026-10-02T14:30:00Z",
  "registroAnulacion": null,
  "accionesDisponibles": ["VER_DESGLOSE", "ANULAR"]
}
```

El cuerpo del `201` es el mismo `LiquidacionDto` que devuelve `GET /api/v1/liquidaciones/{id}`; la cabecera `Location` apunta a esa consulta. A partir de este momento el lote deja de aparecer en `GET /api/v1/lotes` (M3-CU01.FR-014). `fechaSincronizacionM1` y `fechaSincronizacionM2` salen del `snapshot_datos_origen` congelado.

### Consulta de Liquidación Existente (FR-014)

`GET /api/v1/liquidaciones/{id}` devuelve el mismo `LiquidacionDto`. Para una Liquidación `ANULADA`:

```json
GET /api/v1/liquidaciones/40

HTTP/1.1 200 OK
{
  "idLiquidacion": 40,
  "estado": "ANULADA",
  "...": "mismos campos de la Matriz de Venta Final, congelados",
  "registroAnulacion": {
    "idAnulacion": 7,
    "motivo": "Error en el precio por kg ingresado. Se corrige en nueva liquidación.",
    "fechaHoraAnulacion": "2026-10-03T12:00:00Z",
    "usuarioResponsable": "Juan Grau"
  },
  "accionesDisponibles": ["VER_DESGLOSE"]
}
```

### Error — Segunda Liquidación Activa (409 Conflict)

```json
HTTP/1.1 409 Conflict
{
  "type": "https://avicontrol.edu.co/errors/liquidacion-ya-existente",
  "title": "Conflicto de Liquidación",
  "status": 409,
  "detail": "El lote a1b2c3d4-e89b-12d3-a456-426614174000 ya tiene una Liquidación ACTIVA (ID: 42). Anúlela antes de generar una nueva.",
  "instance": "/api/v1/liquidaciones",
  "timestamp": "2026-10-03T11:02:00Z",
  "idLiquidacionActiva": 42
}
```

El frontend usa `idLiquidacionActiva` para ofrecer "Ver liquidación" o "Anular" (CU03 esc. 8).

### Error — Vista Previa Desactualizada (409 Conflict)

```json
HTTP/1.1 409 Conflict
{
  "type": "https://avicontrol.edu.co/errors/vista-previa-desactualizada",
  "title": "Los datos cambiaron",
  "status": 409,
  "detail": "Los datos sincronizados del lote cambiaron después de la vista previa. Revise la nueva vista previa antes de confirmar.",
  "instance": "/api/v1/liquidaciones",
  "timestamp": "2026-10-03T11:03:00Z"
}
```

### Error — Validación del Request (400 Bad Request)

```json
POST /api/v1/liquidaciones/previa
{ "idLote": "a1b2c3d4-e89b-12d3-a456-426614174000", "precioKgCop": 0, "usuario": "Juan Grau" }

HTTP/1.1 400 Bad Request
{
  "type": "https://avicontrol.edu.co/errors/validacion",
  "title": "Error de validación",
  "status": 400,
  "detail": "La solicitud contiene parámetros inválidos.",
  "instance": "/api/v1/liquidaciones/previa",
  "timestamp": "2026-10-03T10:58:00Z",
  "errores": [
    { "campo": "precioKgCop", "mensaje": "El precio por kg debe ser mayor que 0" }
  ]
}
```

Mismo formato para `usuario` vacío, `motivo` fuera de 10–500 caracteres en la anulación y UUID mal formados.

### Error — Liquidación No Encontrada (404 Not Found)

```json
GET /api/v1/liquidaciones/999

HTTP/1.1 404 Not Found
{
  "type": "https://avicontrol.edu.co/errors/liquidacion-no-encontrada",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "No se encontró la liquidación con ID 999.",
  "instance": "/api/v1/liquidaciones/999",
  "timestamp": "2026-10-03T11:05:00Z"
}
```

### Error — Lote No Apto para Liquidar (422 Unprocessable Entity)

```json
HTTP/1.1 422 Unprocessable Entity
{
  "type": "https://avicontrol.edu.co/errors/lote-no-liquidable",
  "title": "Lote no apto para liquidar",
  "status": 422,
  "detail": "El lote no cuenta con resultado final de sacrificio sincronizado desde Módulo 2. No es posible liquidar",
  "instance": "/api/v1/liquidaciones/previa",
  "timestamp": "2026-10-03T11:06:00Z",
  "causa": "SIN_RESULTADO_SACRIFICIO"
}
```

Con `causa = PARTIDAS_SIN_PRECIO` se agrega `partidasPendientes: [{categoria, concepto, referenciaOrigen}]` (CU03 esc. 4). Los textos de `detail` son los de los escenarios de CU03.

### Siniestro Total

```json
POST /api/v1/liquidaciones

{
  "idLote": "b2c3d4e5-e89b-12d3-a456-426614174000",
  "usuario": "Juan Grau",
  "huellaDatos": "1c7e..."
}

HTTP/1.1 201 Created
Location: /api/v1/liquidaciones/43
{
  "idLiquidacion": 43,
  "estado": "ACTIVA",
  "idLote": "b2c3d4e5-e89b-12d3-a456-426614174000",
  "idGalpon": "e2d3c4b5-e89b-12d3-a456-426614174000",
  "nombreLote": "Lote Sur Ciclo 3",
  "nombreGalpon": "Galpón Sur",
  "siniestroTotal": true,
  "precioKgCop": null,
  "pollosVendidos": 0,
  "pesoTotalKg": 0.00,
  "pesoPromedioKg": null,
  "ventaBrutaCop": 0,
  "mortalidadAves": 8000,
  "porcentajeMortalidad": 100.00,
  "subtotalesCategoria": {
    "alimento": 21000000,
    "insumosMedicos": 1800000,
    "costoPoblacion": 10000000
  },
  "costosOperativosCop": 32800000,
  "utilidadNetaCop": -32800000,
  "fechaHoraGeneracion": "2026-10-03T11:20:00Z",
  "usuarioResponsable": "Juan Grau",
  "fechaSincronizacionM1": "2026-10-01T08:00:00Z",
  "fechaSincronizacionM2": "2026-10-02T14:30:00Z",
  "registroAnulacion": null,
  "accionesDisponibles": ["VER_DESGLOSE", "ANULAR"]
}
```

En siniestro total `pollosVendidos = 0` y `pesoTotalKg = 0.00` (valores conocidos: no hubo venta); `precioKgCop`, `pesoPromedioKg` y el resultado de sacrificio son `null` porque no existen.

### Anulación (Request/Response CU04)

```json
PUT /api/v1/liquidaciones/42/anulacion

{
  "motivo": "Error en el precio por kg ingresado. Se corrige en nueva liquidación.",
  "usuario": "Juan Grau"
}

HTTP/1.1 200 OK
{
  "idLiquidacion": 42,
  "estado": "ANULADA",
  "registroAnulacion": {
    "idAnulacion": 7,
    "motivo": "Error en el precio por kg ingresado. Se corrige en nueva liquidación.",
    "fechaHoraAnulacion": "2026-10-03T12:00:00Z",
    "usuarioResponsable": "Juan Grau"
  }
}
```

Anular una Liquidación ya `ANULADA` responde `409 liquidacion-ya-anulada` con el campo extra `registroAnulacion` (CU04 esc. 4).

---

## Snippets de Código Java de Puertos, DTOs y Servicios

### Puertos de Entrada
```java
package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.*;

/** CU03 — Paso 1+2: calcular vista previa sin persistir */
public interface PreviewLiquidacionUseCase {
    LiquidacionPreviaDto calcularVistaPrevia(GenerarLiquidacionCommand comando);
}

/** CU03 — Paso 2 confirmado: persistir la Liquidación */
public interface GenerarLiquidacionUseCase {
    LiquidacionDto generarLiquidacion(GenerarLiquidacionCommand comando);
}

/** CU03.FR-014 — Consultar Liquidación existente */
public interface ConsultarLiquidacionUseCase {
    LiquidacionDto consultarLiquidacion(Long idLiquidacion);
}

/** CU04 — Anular Liquidación ACTIVA */
public interface AnularLiquidacionUseCase {
    AnulacionDto anularLiquidacion(Long idLiquidacion, String motivo, String usuario);
}
```

```java
package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

/** precioKgCop es null en siniestro total; huellaDatos es null en la vista previa. */
public record GenerarLiquidacionCommand(UUID idLote, BigDecimal precioKgCop, String usuario, String huellaDatos) {}
```

### Servicio de Vista Previa + Generación: `GenerarLiquidacionService.java`
```java
package co.edu.unimagdalena.avicontrol.application.service;

@Service
public class GenerarLiquidacionService implements PreviewLiquidacionUseCase, GenerarLiquidacionUseCase {

    // Repositorios de la copia local, LiquidacionRepository, AvisoUtilizacionRepository y Clock inyectados...

    @Override
    @Transactional(readOnly = true)
    public LiquidacionPreviaDto calcularVistaPrevia(GenerarLiquidacionCommand cmd) {
        var datos = cargarYValidar(cmd);                 // precondiciones 2 a 8; no persiste nada
        var calculo = CalculoLiquidacion.de(datos, cmd.precioKgCop());
        return LiquidacionMapper.toPreviaDto(datos, calculo, datos.huella(cmd.precioKgCop()));
    }

    @Override
    @Transactional
    public LiquidacionDto generarLiquidacion(GenerarLiquidacionCommand cmd) {
        var datos = cargarYValidar(cmd);                 // FR-015: las validaciones se repiten al confirmar
        if (!datos.huella(cmd.precioKgCop()).equals(cmd.huellaDatos())) {
            throw new VistaPreviaDesactualizadaException(cmd.idLote());   // precondición 9
        }
        var calculo = CalculoLiquidacion.de(datos, cmd.precioKgCop());
        var liq = Liquidacion.crearActiva(datos, calculo, cmd.usuario(), clock.instant());
        liq = liquidacionRepository.guardar(liq);        // incluye partidas congeladas y snapshot por fuente
                                                         // violación de uq_lote_activa → 409 en el manejador global
        datos.resultadoSacrificio().ifPresent(resultado -> {             // FR-010 / CU08.FR-006
            resultadoSacrificioRepository.marcarUtilizado(resultado.id());
            avisoRepository.crearPendienteSiNoExiste(resultado.id(), liq.id(), clock.instant());
        });
        return LiquidacionMapper.toDto(liq);
    }

    private DatosLiquidacion cargarYValidar(GenerarLiquidacionCommand cmd) {
        var lote = loteRepository.buscarPorId(cmd.idLote())
                .orElseThrow(() -> new LoteNoEncontradoException(cmd.idLote()));
        boolean siniestro = lote.poblacionActual() == 0;            // FR-005: siniestro = población 0
        validarPrecio(siniestro, cmd.precioKgCop());                 // FR-011
        liquidacionRepository.buscarActivaPorLote(lote.id())
                .ifPresent(activa -> { throw new LiquidacionYaExistenteException(lote.id(), activa.id()); });
        var alerta = alertaRepository.buscarPorLote(lote.id())
                .orElseThrow(() -> new LoteNoLiquidableException(CausaNoLiquidable.SIN_ALERTA_VACIADO));
        var resultado = resultadoSacrificioRepository.buscarPorLote(lote.id());
        if (resultado.isEmpty() && !siniestro) {
            throw new LoteNoLiquidableException(CausaNoLiquidable.SIN_RESULTADO_SACRIFICIO);
        }
        var alimento = partidaAlimentoRepository.buscarPorLote(lote.id());
        if (alimento.isEmpty()) {
            throw new LoteNoLiquidableException(CausaNoLiquidable.SIN_PARTIDAS_ALIMENTO);
        }
        var medicamentos = consumoMedicamentoRepository.buscarPorLote(lote.id());
        var pendientes = PartidasPendientes.de(alimento, medicamentos);            // FR-006
        if (!pendientes.isEmpty()) {
            throw new LoteNoLiquidableException(CausaNoLiquidable.PARTIDAS_SIN_PRECIO, pendientes);
        }
        return new DatosLiquidacion(lote, alerta, resultado, alimento, medicamentos, siniestro);
    }
}
```

`CalculoLiquidacion` (dominio) aplica FR-001 a FR-004 y las reglas de partidas de la arquitectura general: Venta Bruta = `HALF_UP(pesoTotalKg × precioKgCop)` (0 en siniestro), mortalidad = `poblacionInicial − poblacionActual`, porcentaje con 2 decimales, costos = suma de subtotales, utilidad = venta − costos.

### Servicio de Anulación: `AnularLiquidacionService.java`
```java
package co.edu.unimagdalena.avicontrol.application.service;

@Service
public class AnularLiquidacionService implements AnularLiquidacionUseCase {

    @Override
    @Transactional  // FR-008: transacción atómica
    public AnulacionDto anularLiquidacion(Long idLiquidacion, String motivo, String usuario) {
        var liq = liquidacionRepository.buscarPorId(idLiquidacion)
            .orElseThrow(() -> new LiquidacionNoEncontradaException(idLiquidacion));

        // FR-002: solo se anula si está ACTIVA; se devuelve el registro existente (esc. 4)
        if (!liq.estaActiva()) {
            throw new LiquidacionYaAnuladaException(idLiquidacion, anulacionRepository.buscarPorLiquidacion(idLiquidacion));
        }

        // FR-003: motivo y usuario llegan recortados y validados desde AnulacionRequest
        // FR-006: solo cambia el estado; la versión (@Version) detecta anulaciones concurrentes → 409
        liq.anular();
        var registro = RegistroAnulacion.crear(liq.id(), motivo, clock.instant(), usuario);
        liquidacionRepository.guardar(liq);
        anulacionRepository.guardar(registro);

        return AnulacionMapper.toDto(liq, registro);
    }
}
```

### Controlador REST: `LiquidacionController.java`
```java
package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

@RestController
@RequestMapping("/api/v1/liquidaciones")
public class LiquidacionController {

    // Casos de uso inyectados por constructor...

    /** CU03 Paso 1+2: generar vista previa sin persistir */
    @PostMapping("/previa")
    public ResponseEntity<LiquidacionPreviaDto> vistaPrevia(@Valid @RequestBody VistaPreviaRequest req) {
        return ResponseEntity.ok(previewUseCase.calcularVistaPrevia(req.toCommand()));
    }

    /** CU03 Paso 2 confirmado: persistir la Liquidación */
    @PostMapping
    public ResponseEntity<LiquidacionDto> generarLiquidacion(@Valid @RequestBody GenerarLiquidacionRequest req) {
        var dto = generarUseCase.generarLiquidacion(req.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/liquidaciones/" + dto.idLiquidacion())).body(dto);
    }

    /** CU03.FR-014: consultar Liquidación existente (ACTIVA o ANULADA) */
    @GetMapping("/{id}")
    public ResponseEntity<LiquidacionDto> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(consultarUseCase.consultarLiquidacion(id));
    }

    /** CU04: anulación (iniciada desde la vista de una Liquidación ACTIVA) */
    @PutMapping("/{id}/anulacion")
    public ResponseEntity<AnulacionDto> anular(@PathVariable Long id, @Valid @RequestBody AnulacionRequest req) {
        return ResponseEntity.ok(anularUseCase.anularLiquidacion(id, req.motivo(), req.usuario()));
    }
}
```

`VistaPreviaRequest`, `GenerarLiquidacionRequest` (añade `huellaDatos` obligatorio) y `AnulacionRequest` validan `usuario` con `@NotBlank @Size(min = 3, max = 100)`, `precioKgCop` con `@Positive` y `motivo` con `@NotBlank @Size(min = 10, max = 500)`. Para que los rangos se midan sin los espacios del inicio y del final (FR-003, [Identidad del usuario](../arquitectura/general.md#identidad-del-usuario)), cada Request recorta `usuario` y `motivo` en su constructor compacto; Jackson construye el record y Bean Validation lo valida después, ya recortado:

```java
public record AnulacionRequest(String motivo, String usuario) {
    public AnulacionRequest {
        motivo = motivo == null ? null : motivo.strip();
        usuario = usuario == null ? null : usuario.strip();
    }
}
```

Las anotaciones van sobre los componentes del record (`@NotBlank @Size(min = 10, max = 500) String motivo`); se omiten aquí para abreviar.

---

## Phase 1: Foundational — Esquema SQL de Liquidación y Dominio

- [ ] **T001** Crear migración Flyway `V3__create_liquidacion_tables.sql` (DDL de la arquitectura general).
- [ ] **T002** Crear modelos de dominio `Liquidacion`, `PartidaCostoLote`, `SnapshotDatosOrigen`, `RegistroAnulacion`, `CalculoLiquidacion`, enums `EstadoLiquidacion`, `CategoriaCosto`, `CausaNoLiquidable` y excepciones de negocio.
- [ ] **T003** Crear entidades JPA (con `@Version` en `LiquidacionEntity`), repositorios Spring Data y adaptadores.
- [ ] **T004** Implementar `Rounding.java` en `domain/shared/` y la traducción de excepciones a `ProblemDetail` en `GlobalExceptionHandler` (incluida la violación de `uq_lote_activa` y `OptimisticLockException`).

---

## Phase 2: CU03 — Cálculo y Vista Previa (sin persistir)

- [ ] **T005** Unit Test `CalculoLiquidacionTest.java`: Caso Dorado de CU03 esc. 1 (107.100.000 / 500 aves / 5,56 % / 22.100.000), ejemplo del plan (139.200.000 / 4,00 %), siniestro total, redondeo `HALF_UP`, costo de población como partida única.
- [ ] **T006** Unit Test `PreviewLiquidacionServiceTest.java`: las precondiciones 1 a 8 con su error (el precio se valida después del `404`), siniestro total por población 0, lote sin resultado y con aves vivas → `SIN_RESULTADO_SACRIFICIO`, huella estable para los mismos datos, y que **no se persiste nada**.
- [ ] **T007** Implementar `PreviewLiquidacionUseCase` y la vista previa en `GenerarLiquidacionService.java`; Controller Test e implementación de `POST /api/v1/liquidaciones/previa` (responde 200, no 201).

---

## Phase 3: CU03 — Confirmación (Persistir Liquidación)

- [ ] **T008** Unit Test `GenerarLiquidacionServiceTest.java`: Caso Dorado, siniestro total, duplicado `409`, huella distinta `409`, re-ejecución de validaciones, partidas congeladas y snapshot por fuente, resultado marcado `UTILIZADO` y aviso `PENDIENTE` creado una sola vez por resultado (también tras anular y re-liquidar).
- [ ] **T009** Implementar `GenerarLiquidacionUseCase` (persistencia real, aviso en la misma transacción).
- [ ] **T010** Integration Test e implementación de `POST /api/v1/liquidaciones` (201 Created, `Location`), incluida la concurrencia de dos confirmaciones simultáneas.

---

## Phase 4: CU03.FR-014 — Consultar Liquidación Existente

- [ ] **T011** Unit Test `ConsultarLiquidacionServiceTest.java`: Liquidación `ACTIVA` (acciones `VER_DESGLOSE` y `ANULAR`), `ANULADA` (con `registroAnulacion` y solo `VER_DESGLOSE`), no encontrada (404); los valores salen congelados, sin recalcular.
- [ ] **T012** Implementar `ConsultarLiquidacionUseCase` y `ConsultarLiquidacionService.java`.
- [ ] **T013** Controller Test e implementación de `GET /api/v1/liquidaciones/{id}`.

---

## Phase 5: CU04 — Anular Liquidación

- [ ] **T014** Unit Test `AnularLiquidacionServiceTest.java`: anulación exitosa con motivo y usuario, rechazo de motivo < 10 caracteres o solo espacios, rechazo sobre `ANULADA` con el registro existente, atributos distintos del estado sin cambios, transacción atómica (rollback ante fallo).
- [ ] **T015** Implementar `AnularLiquidacionService.java` y `AnularLiquidacionUseCase.java`.
- [ ] **T016** Integration Test e implementación de `PUT /api/v1/liquidaciones/{id}/anulacion`, incluida la anulación concurrente (la segunda recibe `409 liquidacion-ya-anulada`) y la reaparición del lote en `GET /api/v1/lotes`.
