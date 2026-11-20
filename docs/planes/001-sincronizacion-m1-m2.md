# Implementation Plan: Sincronización de Datos Locales (M1 y M2)

**Date**: 2026-09-28  
**Actualizado**: 2026-11-20 (SC-06: la integración pasa de eventos Kafka a consultas REST)  
**Arquitectura, DDL y reglas de consulta**: [general.md](../arquitectura/general.md)  
**Specs**:
- [m3-cu07-consultar-galpon-lote-m1](../features/m3-cu07-consultar-galpon-lote-m1/spec.md) – Consultar Galpón y Lote al M1  
- [m3-cu08-consultar-resultado-sacrificio-m2](../features/m3-cu08-consultar-resultado-sacrificio-m2/spec.md) – Consultar Resultado Final de Sacrificio al M2  
- [m3-cu09-consultar-alimento-requerido-m2](../features/m3-cu09-consultar-alimento-requerido-m2/spec.md) – Consultar Alimento Requerido al M2  
- [m3-cu10-consultar-consumo-medicamento-m2](../features/m3-cu10-consultar-consumo-medicamento-m2/spec.md) – Consultar Consumo de Medicamento al M2  

## Summary

Este subsistema mantiene la **copia local sincronizada** de M3 consultando por **REST** a Módulo 1 (galpones, lotes y, derivada de ellos, la alerta de vaciado sanitario) y a Módulo 2 (resultados finales de sacrificio, partidas de alimento y consumos de medicamento con sus tramos por recepción). Una tarea programada hace las consultas al arrancar y cada cierto intervalo; la lista de lotes y la generación de Liquidaciones solo leen la copia local. Cada ejecución queda en la bitácora `registro_sincronizacion`. El aviso de utilización del resultado de sacrificio se entrega a Módulo 2 con el patrón *outbox*.

**Por qué REST y no Kafka.** Verificado en los repositorios y servicios desplegados de los otros módulos (noviembre de 2026): M1 no publica eventos (su Kafka es solo entre M2 y M1) y expone el REST de consulta de su [acuerdo de contratos](https://github.com/AviControl-org/AVICONTROL/blob/develop/avicontrol/docs/plan-general/acuerdo-contratos-entre-modulos.md); M2 no publica nada hacia M3 y expone REST. Ver la solicitud SC-06 en el [control de cambios](../proceso/control-cambios.md).

## Technical Context

- **Language/Version**: Java 21 (LTS)
- **Primary Dependencies**: Spring Boot 4.1.x, `RestClient` (Spring Web), Spring Data JPA, Jakarta Validation, `@Scheduled`
- **Storage**: PostgreSQL, migraciones Flyway `V1__create_sync_tables.sql` y `V2__create_partidas_tables.sql` (DDL en la arquitectura general)
- **Testing**: JUnit 5 y Mockito (servicios), servidor HTTP simulado con los JSON del acuerdo (adaptadores REST), Spring Boot Test + Testcontainers con PostgreSQL
- **Target Platform**: JVM 21 / Docker
- **Project Type**: Backend service (capa de sincronización e integración)
- **Configuración**: `M1_BASE_URL` (por ejemplo `http://18.227.58.143/api/v1`), `SYNC_M1_INTERVALO` (por defecto `PT5M`); para M2, `M2_BASE_URL`, `M2_USUARIO` y `M2_CLAVE`. Ninguna va en el código.
- **Performance Goals**: una sincronización completa de M1 con pocos galpones en menos de 10 s; tiempos máximos de 2 s de conexión y 5 s de lectura por consulta, sin reintentos dentro de una misma consulta (el siguiente ciclo reintenta)

---

## Contratos con Módulo 1 (REST, según su acuerdo)

Solo lectura, sin autenticación, base `/api/v1`. Los nombres de campo son los de M1 y no se traducen en el contrato: el adaptador de M3 los traduce a los de la copia local (`capacidad` → `aforo_maximo`, `loteActivo` → lote `ACTIVO`, `costoTotal` → `costo_total_cop`). Los errores usan RFC 9457 (`application/problem+json`) con un `code`.

### A. `GET /api/v1/galpones?page={n}` — lista de galpones (CU07)

Tamaño de página fijo de 10 (no existe `size`). Se recorren las páginas hasta `totalPaginas`.

```json
{
  "contenido": [
    { "id": "550e8400-e29b-41d4-a716-446655440000", "nombre": "Galpón Norte", "capacidad": 1500, "estado": "PRODUCTIVO" }
  ],
  "pagina": 1,
  "tamanio": 10,
  "totalElementos": 1,
  "totalPaginas": 1,
  "sinCoincidencias": false
}
```

### B. `GET /api/v1/galpones/{galponId}` — galpón y su lote activo (CU07)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nombre": "Galpón Norte",
  "capacidad": 1500,
  "estado": "PRODUCTIVO",
  "loteActivo": {
    "id": "2fc03a21-84c4-4c83-bb15-6607bb75cb90",
    "nombre": "Lote A1",
    "poblacionInicial": 1000,
    "poblacionActual": 975,
    "fechaIngreso": "2026-09-15",
    "edadDias": 21,
    "costoTotal": 5000000
  }
}
```

Un galpón sin lote activo responde `200` **sin** el campo `loteActivo` (nunca `null`). `mensaje` e `incidencias[{codigo, mensaje}]` son informativos, no errores.

### C. `GET /api/v1/galpones/{galponId}/lotes` — historial de lotes (CU07)

```json
{
  "galponId": "550e8400-e29b-41d4-a716-446655440000",
  "loteActivo": { "id": "…", "nombre": "…", "poblacionInicial": 1000, "poblacionActual": 975, "fechaIngreso": "2026-09-15", "edadDias": 21, "costoTotal": 5000000 },
  "lotesAnteriores": [
    { "id": "7331eec8-e447-4868-bdef-8eca3fff8d44", "nombre": "lote demo", "poblacionInicial": 2000, "poblacionActual": 1974, "fechaIngreso": "2026-11-18", "edadDias": 2, "costoTotal": 5000000 }
  ]
}
```

Según el acuerdo de M1, un lote deja de ser activo únicamente cuando M1 acepta el vaciado sanitario. Por eso **aparecer en `lotesAnteriores` equivale a tener alerta de vaciado**: M3 deriva la alerta (ver la regla de derivación abajo). M1 no expone el id ni la hora de la alerta.

### Errores de M1

| HTTP | `code` | Caso | Tratamiento en M3 |
|---|---|---|---|
| 400 | `PARAMETRO_INVALIDO` | `page` o `galponId` inválidos | Ejecución `FALLIDA`; no se reintenta dentro del ciclo |
| 404 | `GALPON_NO_ENCONTRADO` | El galpón ya no existe en M1 | Se omite ese galpón; la copia local no se borra |
| 500 | `ERROR_CONSULTA` | Error técnico de M1 | Ejecución `FALLIDA`; reintenta el siguiente ciclo |
| — | — | M1 no responde (tiempo agotado) | Ejecución `FALLIDA`; la copia local queda intacta |

### Reglas de derivación

- **Estado del lote**: `ACTIVO` si viene como `loteActivo`; `DESVINCULADO` si viene en `lotesAnteriores`.
- **Alerta de vaciado**: por cada lote en `lotesAnteriores` que no tenga alerta, se inserta una con `id_alerta` determinista (UUID calculado del `id` del lote), `fecha_hora_evento` = momento en que M3 la detectó y `id_registro_sincronizacion` = la ejecución actual. Las alertas son inmutables. Sin migración: usa las columnas existentes.
- **Datos inválidos** (CU07.FR-010, FR-011): un lote sin `poblacionInicial`, con `costoTotal <= 0` o con `poblacionActual > poblacionInicial` se omite y se anota en `descripcion_error`; no sobrescribe la copia local.

---

## Contratos con Módulo 2 (REST, por confirmar — D-08)

M2 todavía no publica para M3. M3 propone consultar por lote, solo los lotes con alerta de vaciado y sin Liquidación activa. Los `payload` siguientes son la **forma que M3 necesita**; los endpoints, la autenticación (Basic hoy) y los campos que hoy faltan se acuerdan con M2. Endpoints actuales de M2 que cubren parte de esto: `GET /api/ordenes-sacrificio/{ordenId}/resultados`, `GET /api/lotes/{loteId}/consumos-medicamento` y `GET /api/lotes/{loteId}/alimento-requerido`.

### D. Resultado de sacrificio (CU08)

```json
{
  "idResultado": "456e7890-e89b-12d3-a456-426614174000",
  "idLote": "98765432-e89b-12d3-a456-426614174000",
  "idGalpon": "123e4567-e89b-12d3-a456-426614174000",
  "cantidadFinalPollos": 8500,
  "pesoTotalKg": 23800.00,
  "fechaRegistro": "2026-09-25T18:00:00-05:00"
}
```

### E. Partidas de alimento (CU09)

```json
{
  "idPartida": "777e4567-e89b-12d3-a456-426614174000",
  "idLote": "98765432-e89b-12d3-a456-426614174000",
  "idGalpon": "123e4567-e89b-12d3-a456-426614174000",
  "tipoAlimento": "Engorde",
  "cantidadKg": 12500.000,
  "precioUnitarioKgCop": 2400.00,
  "valorImpuestoCop": 1500000.00,
  "referenciaOrigen": "REC-2026-118"
}
```

`precioUnitarioKgCop` puede faltar: la partida se guarda con `estado_valorizacion = 'PENDIENTE_PRECIO'` (CU09.FR-005).

### F. Consumos de medicamento (CU10)

```json
{
  "idConsumo": "333e4567-e89b-12d3-a456-426614174000",
  "idLote": "98765432-e89b-12d3-a456-426614174000",
  "idGalpon": "123e4567-e89b-12d3-a456-426614174000",
  "medicamento": "Amoxicilina",
  "fechaConsumo": "2026-08-20",
  "cantidadUnidadBase": 50.000,
  "unidadBase": "ML",
  "tramos": [
    { "referenciaRecepcion": "REC-2026-090", "cantidadUnidadBase": 30.000, "precioUnitarioBaseCop": 20000.00, "valorImpuestoCop": 30000.00 },
    { "referenciaRecepcion": "REC-2026-101", "cantidadUnidadBase": 20.000, "precioUnitarioBaseCop": 21000.00, "valorImpuestoCop": 21000.00 }
  ]
}
```

M2 entrega cantidad y precio por tramo, nunca un total en pesos (RT-07). Un tramo sin `precioUnitarioBaseCop` se guarda `PENDIENTE_PRECIO` (CU10.FR-006). Al reprocesar un consumo se reemplazan sus tramos.

### G. Aviso de utilización (CU08, saliente M3 → M2)

```json
{
  "idResultado": "456e7890-e89b-12d3-a456-426614174000",
  "idLote": "98765432-e89b-12d3-a456-426614174000",
  "idLiquidacion": 42,
  "fechaHoraUtilizacion": "2026-10-03T11:00:00Z"
}
```

El canal (REST con un `POST` a M2, o un tópico de Kafka alojado por M2) se acuerda con M2 (D-03). M1 no participa. Hoy el consumidor de M2 espera el tópico `modulo3.resultado-sacrificio-utilizado.v1` con el sobre `eventId`, `eventType`, `eventVersion`, `aggregateId` y `payload{resultadoId, loteId}`; si el canal es Kafka, M3 enviará exactamente ese sobre.

### Validaciones de contenido (ejecución `FALLIDA` o elemento omitido, sin reintento)

| Dato | Se rechaza si | Spec |
|---|---|---|
| Galpón | Falta `id` o `estado`; `estado` fuera del catálogo `EstadoGalpon` | CU07.FR-010 |
| Lote | Sin `poblacionInicial`, `costoTotal <= 0` o `poblacionActual > poblacionInicial` | CU07.FR-011 |
| Resultado | `cantidadFinalPollos <= 0`, `pesoTotalKg <= 0` o ausentes; ya existe otro resultado para el lote | CU08.FR-003, FR-010 |
| Resultado (corrección) | El resultado existente ya está `UTILIZADO`, aunque su Liquidación se haya anulado (se registra incidencia y no se actualiza) | CU08.FR-004, FR-005 |
| Partida / consumo | Falta `idLote` o cantidad `<= 0` | CU09, CU10 |

**Lote inexistente.** Con REST no hay orden de llegada: M3 sincroniza primero galpones y lotes de M1, y solo después consulta a M2 los lotes que ya existen en la copia local. Por eso no hay eventos "pendientes de lote" ni la tabla `evento_pendiente_lote`.

---

## Snippets de Código Java

### Puerto de salida: `ConsultarGalponesM1.java`
```java
package co.edu.unimagdalena.avicontrol.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultarGalponesM1 {
    /** Todas las páginas de GET /galpones. Lanza ConsultaM1Exception si M1 falla o no responde. */
    List<GalponM1> listarGalpones();

    /** GET /galpones/{id}; vacío si M1 responde 404. */
    Optional<GalponM1> detalle(UUID idGalpon);

    /** GET /galpones/{id}/lotes: lote activo (opcional) y lotes anteriores. */
    HistorialLotesM1 historial(UUID idGalpon);
}
```

### Puerto de entrada: `SincronizarModulo1UseCase.java`
```java
package co.edu.unimagdalena.avicontrol.domain.port.in;

public interface SincronizarModulo1UseCase {
    /** CU07 — Consulta a M1 y actualiza galpones, lotes y alertas derivadas. Una fila en la bitácora por ejecución. */
    ResultadoSincronizacion sincronizar();
}
```

`SincronizarModulo2UseCase` sigue el mismo patrón con `sincronizarLote(UUID idLote)` (resultado, partidas de alimento y consumos).

### Tarea programada: `SincronizacionM1Job.java`
```java
package co.edu.unimagdalena.avicontrol.infrastructure.adapter.scheduling;

@Component
public class SincronizacionM1Job {

    private final SincronizarModulo1UseCase sincronizar;

    // Constructor...

    /** Al arrancar (initialDelay 0) y luego cada ${sync.m1.intervalo}. Nunca lanza: un fallo queda en la bitácora. */
    @Scheduled(initialDelay = 0, fixedDelayString = "${sync.m1.intervalo:PT5M}")
    public void ejecutar() {
        sincronizar.sincronizar();
    }
}
```

### Servicio de Sincronización: `SyncModulo1Service.java`
```java
@Service
public class SyncModulo1Service implements SincronizarModulo1UseCase {

    private final ConsultarGalponesM1 m1;
    private final GalponRepository galponRepository;
    private final LoteRepository loteRepository;
    private final AlertaVaciadoSanitarioRepository alertaRepository;
    private final RegistroSincronizacionRepository bitacora;
    private final Clock clock;

    // Constructor con inyección de dependencias...

    @Override
    public ResultadoSincronizacion sincronizar() {
        var registro = bitacora.iniciar(FuenteSincronizacion.MODULO_1, "GET /api/v1/galpones", clock.instant());
        try {
            int filas = 0;
            for (var resumen : m1.listarGalpones()) {
                filas += sincronizarGalpon(registro, resumen.id());   // una transacción por galpón
            }
            bitacora.completar(registro, filas, clock.instant());
        } catch (ConsultaM1Exception e) {
            bitacora.fallar(registro, e.getMessage(), clock.instant()); // la copia local queda intacta
        }
        return bitacora.resultado(registro);
    }
}
```

`sincronizarGalpon` hace el `upsert` del galpón, del lote activo (`ACTIVO`) y de cada lote anterior (`DESVINCULADO`), e inserta la alerta derivada si no existe, todo en una transacción. Repetir la sincronización no cambia nada (`upsert` por `id_galpon` e `id_lote`).

### Publicación de avisos: `PublicarAvisosUtilizacionService.java`
```java
@Service
public class PublicarAvisosUtilizacionService {

    private final AvisoUtilizacionRepository avisoRepository;
    private final AvisoUtilizacionPublisher publisher;

    // Constructor...

    /** CU08.FR-007: asíncrono, reintentable e idempotente (clave = idResultado). */
    @Scheduled(fixedDelayString = "${avisos.intervalo:PT1M}")
    public void publicarPendientes() {
        for (var aviso : avisoRepository.buscarPendientes()) {
            boolean entregado = publisher.publicar(aviso);   // espera la confirmación de M2
            avisoRepository.registrarIntento(aviso.id(), entregado);
        }
    }
}
```

---

## Phase 1: Foundational – Esquema SQL y clientes REST

- [ ] **T001** Crear migración Flyway `V1__create_sync_tables.sql` (DDL de la arquitectura general).
- [ ] **T002** Crear migración Flyway `V2__create_partidas_tables.sql`.
- [ ] **T003** Crear modelos de dominio (`Galpon`, `Lote`, `AlertaVaciadoSanitario`, `ResultadoFinalSacrificio`, `PartidaAlimentoLote`, `ConsumoMedicamentoLote`, `TramoRecepcionConsumo`, `RegistroSincronizacion`, `AvisoUtilizacionResultado`) y enums (`EstadoGalpon`, `FuenteSincronizacion`, `ResultadoSincronizacion`, `EstadoUtilizacion`, `EstadoValorizacion`, `EstadoEntregaAviso`).
- [ ] **T004** Crear entidades JPA, repositorios Spring Data, adaptadores de los puertos `port/out` y mappers.
- [ ] **T005** Configurar los clientes REST en `RestClientConfig.java`: `RestClient` por módulo con su URL base de `application.yml` (`m1.base-url`, `m2.base-url`), tiempos de 2 s de conexión y 5 s de lectura, y la traducción de `application/problem+json` a `ConsultaM1Exception`.
- [ ] **T006** Crear los DTOs de respuesta de `infrastructure/adapter/rest/cliente/` y los fixtures de `src/test/resources/fixtures/m1/` y `fixtures/m2/` (los mismos JSON del acuerdo).

---

## Phase 2: CU07 – Sincronización M1 (Galpones, Lotes y Alertas)

- [ ] **T007** Unit Test `SyncModulo1ServiceTest.java`: alta y actualización de galpón y lote, población inicial sin cambios en actualizaciones, lote desvinculado conservado, alerta derivada nueva e idempotente, galpón sin lote activo, galpón que M1 ya no devuelve (404, la copia no se borra), datos inválidos omitidos sin sobrescribir, M1 caído (ejecución `FALLIDA` y copia intacta).
- [ ] **T008** Implementar `SincronizarModulo1UseCase`, `SyncModulo1Service.java` y el puerto `ConsultarGalponesM1`.
- [ ] **T009** Integration Test `ClienteM1RestIT.java` (servidor HTTP simulado con los JSON del acuerdo) e implementación de `ClienteM1Rest.java`, incluida la paginación con `page`, el `200` sin `loteActivo` y los errores `problem+json`.
- [ ] **T009b** Implementar `SincronizacionM1Job.java` y la configuración `sync.m1.intervalo` y `m1.base-url`.

---

## Phase 3: CU08 – Resultado de Sacrificio y Aviso de Utilización

- [ ] **T010** Unit Test `SyncModulo2ServiceTest.java` (resultado): resultado válido, inválido, corrección antes de ser utilizado, corrección rechazada después de ser utilizado (también si la Liquidación se anuló), segundo resultado para el mismo lote.
- [ ] **T011** Implementar `sincronizarLote` en `SyncModulo2Service.java` para el resultado. Depende de la respuesta de M2 (D-08).
- [ ] **T012** Unit Test `PublicarAvisosUtilizacionServiceTest.java`: entrega exitosa, fallo que deja el aviso `PENDIENTE` e incrementa `intentos`.
- [ ] **T013** Implementar `PublicarAvisosUtilizacionService.java`, el puerto `AvisoUtilizacionPublisher` y el adaptador del canal acordado con M2 (D-03). La creación del aviso en la misma transacción de la Liquidación se implementa en el plan 003 (T009).

---

## Phase 4: CU09 y CU10 – Alimento y Medicamentos

- [ ] **T014** Unit Tests de `SyncModulo2Service` para partidas de alimento (valorizada, `PENDIENTE_PRECIO`, idempotencia) y consumos de medicamento (uno y varios tramos, tramo sin precio, reemplazo de tramos al reprocesar).
- [ ] **T015** Implementar la parte de alimento y medicamento de `sincronizarLote`.
- [ ] **T016** Integration Test `ClienteM2RestIT.java` e implementación de `ClienteM2Rest.java` (Basic Auth con `M2_USUARIO` y `M2_CLAVE`).

---

## Phase 5: Estado de la Sincronización (RT-09)

- [ ] **T017** Implementar `GET /api/v1/sincronizacion/estado` con el puerto `ConsultarEstadoSincronizacionUseCase`, `EstadoSincronizacionService` (`application/service/sync/`) y `SincronizacionController` (`adapter/rest/`). `ListarLotesService` (plan 002) usa el mismo puerto. El endpoint responde por fuente la fecha y hora de la última sincronización exitosa y `desactualizada: true` si supera `sync.umbral-inactividad` o si la última ejecución falló.

```json
GET /api/v1/sincronizacion/estado

HTTP/1.1 200 OK
{
  "modulo1": { "ultimaSincronizacionExitosa": "2026-10-03T10:00:00Z", "desactualizada": false },
  "modulo2": { "ultimaSincronizacionExitosa": "2026-10-01T22:40:00Z", "desactualizada": true }
}
```
