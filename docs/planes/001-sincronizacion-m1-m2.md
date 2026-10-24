# Implementation Plan: Sincronización de Datos Locales (M1 y M2)

**Date**: 2026-09-28  
**Actualizado**: 2026-10-23  
**Arquitectura, DDL y reglas de consumo**: [general.md](../arquitectura/general.md)  
**Specs**:
- [m3-cu07-consultar-galpon-lote-m1](../features/m3-cu07-consultar-galpon-lote-m1/spec.md) – Consultar Galpón y Lote al M1  
- [m3-cu08-consultar-resultado-sacrificio-m2](../features/m3-cu08-consultar-resultado-sacrificio-m2/spec.md) – Consultar Resultado Final de Sacrificio al M2  
- [m3-cu09-consultar-alimento-requerido-m2](../features/m3-cu09-consultar-alimento-requerido-m2/spec.md) – Consultar Alimento Requerido al M2  
- [m3-cu10-consultar-consumo-medicamento-m2](../features/m3-cu10-consultar-consumo-medicamento-m2/spec.md) – Consultar Consumo de Medicamento al M2  

## Summary

Este subsistema mantiene la **copia local sincronizada** de M3 consumiendo los eventos que Módulo 1 (galpones, lotes y alertas de vaciado sanitario) y Módulo 2 (resultados finales de sacrificio, partidas de alimento y consumos de medicamento con sus tramos por recepción) publican en **Apache Kafka**. El procesamiento es idempotente, cada evento queda en la bitácora `registro_sincronizacion` y el aviso de utilización del resultado de sacrificio se publica hacia Módulo 2 con el patrón *outbox*.

M3 no hace llamadas REST a M1 ni a M2. Los tópicos, reglas de consumo, reintentos y DLT están en la [arquitectura general](../arquitectura/general.md#integración-con-módulo-1-y-módulo-2-apache-kafka).

## Technical Context

- **Language/Version**: Java 21 (LTS)
- **Primary Dependencies**: Spring Boot 4.1.x, Spring Kafka, Spring Data JPA, Jakarta Validation, `@Scheduled` (solo para publicar avisos pendientes)
- **Storage**: PostgreSQL, migraciones Flyway `V1__create_sync_tables.sql` y `V2__create_partidas_tables.sql` (DDL en la arquitectura general)
- **Testing**: JUnit 5 y Mockito (servicios), Spring Boot Test + Testcontainers con PostgreSQL y Kafka (consumidores y productor)
- **Target Platform**: JVM 21 / Docker
- **Project Type**: Backend service (capa de sincronización e integración)
- **Performance Goals**: Procesamiento de un evento < 1 s; reintentos con backoff ante errores técnicos

---

## Contratos de Eventos Kafka

Todos los eventos usan el sobre `header` + `payload` de la arquitectura general. Aquí se muestra solo el `payload`. Los nombres de campo deben confirmarse con M1 y M2 (D-08); el consumidor traduce cualquier diferencia de nombres o de grafía (D-07) a los DTOs de `domain/port/in/dto/evento/`.

### A. `avicontrol.m1.galpones` — `GalponActualizadoEvent` (CU07)

```json
{
  "idGalpon": "123e4567-e89b-12d3-a456-426614174000",
  "nombre": "Galpón Norte",
  "aforoMaximo": 10000,
  "estado": "PRODUCTIVO",
  "loteVinculado": {
    "idLote": "98765432-e89b-12d3-a456-426614174000",
    "nombre": "Lote L-2026-A",
    "estado": "ACTIVO",
    "fechaIngreso": "2026-08-01",
    "poblacionInicial": 9000,
    "poblacionActual": 8500,
    "costoTotalCop": 18000000
  }
}
```

`loteVinculado` es `null` cuando el galpón no tiene lote (`DISPONIBLE`, `MANTENIMIENTO`). Un lote desvinculado conserva su fila local: no se borra al recibir un evento del galpón sin lote.

### B. `avicontrol.m1.alertas-vaciado` — `AlertaVaciadoSanitarioEvent` (CU07)

```json
{
  "idAlerta": "c0ffee00-e89b-12d3-a456-426614174000",
  "idGalpon": "123e4567-e89b-12d3-a456-426614174000",
  "idLote": "98765432-e89b-12d3-a456-426614174000",
  "fechaHoraEvento": "2026-09-26T07:15:00-05:00"
}
```

### C. `avicontrol.m2.resultados-sacrificio` — `ResultadoSacrificioPublicadoEvent` (CU08)

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

### D. `avicontrol.m2.partidas-alimento` — `PartidaAlimentoPublicadaEvent` (CU09)

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

`precioUnitarioKgCop` puede llegar `null`: la partida se guarda con `estado_valorizacion = 'PENDIENTE_PRECIO'` (CU09.FR-005).

### E. `avicontrol.m2.consumos-medicamento` — `ConsumoMedicamentoPublicadoEvent` (CU10)

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

M2 entrega cantidad y precio por tramo, nunca un total en pesos (RT-07). Un tramo con `precioUnitarioBaseCop = null` se guarda `PENDIENTE_PRECIO` (CU10.FR-006). Al reprocesar un consumo se reemplazan sus tramos por los del evento.

### F. `avicontrol.m3.avisos-utilizacion` — `ResultadoUtilizadoEvent` (CU08, saliente)

```json
{
  "idResultado": "456e7890-e89b-12d3-a456-426614174000",
  "idLote": "98765432-e89b-12d3-a456-426614174000",
  "idLiquidacion": 42,
  "fechaHoraUtilizacion": "2026-10-03T11:00:00Z"
}
```

### Validaciones de contenido (evento `FALLIDA`, sin reintento)

| Evento | Se rechaza si | Spec |
|---|---|---|
| Galpón | Falta `idGalpon` o `estado`; `estado` fuera del catálogo `EstadoGalpon`; lote sin `poblacionInicial`, con `costoTotalCop <= 0` o `poblacionActual > poblacionInicial` | CU07.FR-010, FR-011 |
| Alerta | Falta `idLote` o `idGalpon`; el lote no existe en la copia local | CU07.FR-010 |
| Resultado | `cantidadFinalPollos <= 0`, `pesoTotalKg <= 0` o ausentes; ya existe otro resultado para el lote | CU08.FR-003, FR-010 |
| Resultado (corrección) | El resultado existente ya está `UTILIZADO` (se registra incidencia y no se actualiza) | CU08.FR-004, FR-005 |
| Partida / consumo | Cantidad `<= 0` o lote inexistente | CU09, CU10 |

---

## Snippets de Código Java

### Puerto de Entrada: `SincronizarModulo1UseCase.java`
```java
package co.edu.unimagdalena.avicontrol.domain.port.in;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.evento.AlertaVaciadoEvento;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.evento.GalponEvento;
import java.util.UUID;

public interface SincronizarModulo1UseCase {
    /** CU07 — Upsert del galpón y, si viene, de su lote vinculado. */
    void procesarGalpon(UUID idEvento, GalponEvento evento);

    /** CU07 — Inserta la alerta si no existe; las alertas son inmutables. */
    void procesarAlertaVaciado(UUID idEvento, AlertaVaciadoEvento evento);
}
```

`SincronizarModulo2UseCase` sigue el mismo patrón con `procesarResultadoSacrificio`, `procesarPartidaAlimento` y `procesarConsumoMedicamento`.

### Consumidor Kafka: `Modulo1EventConsumer.java`
```java
package co.edu.unimagdalena.avicontrol.infrastructure.adapter.messaging;

import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo1UseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class Modulo1EventConsumer {

    private final SincronizarModulo1UseCase sincronizarModulo1;
    private final EventoMapper mapper;

    public Modulo1EventConsumer(SincronizarModulo1UseCase sincronizarModulo1, EventoMapper mapper) {
        this.sincronizarModulo1 = sincronizarModulo1;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${kafka.topicos.m1-galpones}", groupId = "avicontrol-modulo3")
    public void consumirGalpon(SobreEvento<GalponPayload> sobre, Acknowledgment ack) {
        sincronizarModulo1.procesarGalpon(sobre.header().eventId(), mapper.toGalponEvento(sobre.payload()));
        ack.acknowledge(); // solo después de confirmar la transacción
    }

    @KafkaListener(topics = "${kafka.topicos.m1-alertas-vaciado}", groupId = "avicontrol-modulo3")
    public void consumirAlerta(SobreEvento<AlertaVaciadoPayload> sobre, Acknowledgment ack) {
        sincronizarModulo1.procesarAlertaVaciado(sobre.header().eventId(), mapper.toAlertaEvento(sobre.payload()));
        ack.acknowledge();
    }
}
```

### Servicio de Sincronización: `SyncModulo1Service.java`
```java
package co.edu.unimagdalena.avicontrol.application.service.sync;

import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo1UseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.evento.AlertaVaciadoEvento;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.evento.GalponEvento;
import co.edu.unimagdalena.avicontrol.domain.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class SyncModulo1Service implements SincronizarModulo1UseCase {

    private final GalponRepository galponRepository;
    private final LoteRepository loteRepository;
    private final AlertaVaciadoSanitarioRepository alertaRepository;
    private final RegistroSincronizacionRepository bitacora;
    private final Clock clock;

    // Constructor con inyección de dependencias...

    @Override
    @Transactional
    public void procesarGalpon(UUID idEvento, GalponEvento evento) {
        if (bitacora.yaProcesado(idEvento)) return;               // idempotencia por eventId
        var registro = bitacora.iniciar(FuenteSincronizacion.MODULO_1, "m1.galpones", idEvento, clock.instant());

        var errores = evento.validar();                           // CU07.FR-010 y FR-011
        if (!errores.isEmpty()) {
            bitacora.fallar(registro, String.join("; ", errores), clock.instant());
            return;                                               // no sobrescribe la copia local
        }
        galponRepository.guardarOActualizar(evento.toGalpon(clock.instant()));
        int filas = 1;
        if (evento.loteVinculado() != null) {
            loteRepository.guardarOActualizar(evento.toLote(clock.instant()));
            filas++;
        }
        bitacora.completar(registro, filas, clock.instant());
    }

    @Override
    @Transactional
    public void procesarAlertaVaciado(UUID idEvento, AlertaVaciadoEvento evento) {
        if (bitacora.yaProcesado(idEvento)) return;
        var registro = bitacora.iniciar(FuenteSincronizacion.MODULO_1, "m1.alertas-vaciado", idEvento, clock.instant());
        // Inmutable: si ya existe una alerta para el lote no se modifica (CU07 esc. 6)
        int filas = alertaRepository.guardarSiNoExiste(evento.toAlerta(registro.id())) ? 1 : 0;
        bitacora.completar(registro, filas, clock.instant());
    }
}
```

Los errores técnicos (base de datos, JSON ilegible) se propagan como excepción para que el `DefaultErrorHandler` de Spring Kafka aplique los reintentos y la DLT.

### Publicación de avisos: `PublicarAvisosUtilizacionService.java`
```java
package co.edu.unimagdalena.avicontrol.application.service.sync;

import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionPublisher;
import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PublicarAvisosUtilizacionService {

    private final AvisoUtilizacionRepository avisoRepository;
    private final AvisoUtilizacionPublisher publisher;

    // Constructor...

    /** CU08.FR-007: asíncrono, reintentable e idempotente (clave = idResultado). */
    @Scheduled(fixedDelayString = "${avisos.intervalo:PT1M}")
    public void publicarPendientes() {
        for (var aviso : avisoRepository.buscarPendientes()) {
            boolean entregado = publisher.publicar(aviso);   // espera la confirmación del broker
            avisoRepository.registrarIntento(aviso.id(), entregado);
        }
    }
}
```

---

## Phase 1: Foundational – Esquema SQL e Infraestructura Kafka

- [ ] **T001** Crear migración Flyway `V1__create_sync_tables.sql` (DDL de la arquitectura general).
- [ ] **T002** Crear migración Flyway `V2__create_partidas_tables.sql`.
- [ ] **T003** Crear modelos de dominio (`Galpon`, `Lote`, `AlertaVaciadoSanitario`, `ResultadoFinalSacrificio`, `PartidaAlimentoLote`, `ConsumoMedicamentoLote`, `TramoRecepcionConsumo`, `RegistroSincronizacion`, `AvisoUtilizacionResultado`) y enums (`EstadoGalpon`, `FuenteSincronizacion`, `ResultadoSincronizacion`, `EstadoUtilizacion`, `EstadoValorizacion`, `EstadoEntregaAviso`).
- [ ] **T004** Crear entidades JPA, repositorios Spring Data, adaptadores de los puertos `port/out` y mappers.
- [ ] **T005** Configurar Kafka en `KafkaConfig.java`: consumidor con confirmación manual, `DefaultErrorHandler` con 3 reintentos y backoff exponencial, `DeadLetterPublishingRecoverer` hacia `avicontrol.m3.dlt`, productor con `acks=all`. Tópicos en `application.yml` (`kafka.topicos.*`).
- [ ] **T006** Crear el sobre `SobreEvento<T>` y los payloads JSON de `infrastructure/adapter/messaging/`, y los fixtures de `src/test/resources/fixtures/m1/` y `fixtures/m2/`.

---

## Phase 2: CU07 – Sincronización M1 (Galpones, Lotes y Alertas)

- [ ] **T007** Unit Test `SyncModulo1ServiceTest.java`: alta y actualización de galpón y lote, población inicial sin cambios en actualizaciones, lote desvinculado conservado, alerta nueva e idempotente, evento repetido descartado por `eventId`, datos inválidos registrados como `FALLIDA` sin sobrescribir.
- [ ] **T008** Implementar `SincronizarModulo1UseCase` y `SyncModulo1Service.java`.
- [ ] **T009** Integration Test `Modulo1EventConsumerIT.java` (Testcontainers) e implementación de `Modulo1EventConsumer.java`, incluida la traducción de la grafía de estados de M1 (D-07).

---

## Phase 3: CU08 – Resultado de Sacrificio y Aviso de Utilización

- [ ] **T010** Unit Test `SyncModulo2ServiceTest.java` (resultado): resultado válido, inválido, corrección antes y después de ser utilizado, segundo resultado para el mismo lote.
- [ ] **T011** Implementar `procesarResultadoSacrificio` en `SyncModulo2Service.java`.
- [ ] **T012** Unit Test `PublicarAvisosUtilizacionServiceTest.java`: entrega exitosa, fallo que deja el aviso `PENDIENTE` e incrementa `intentos`.
- [ ] **T013** Implementar `PublicarAvisosUtilizacionService.java`, el puerto `AvisoUtilizacionPublisher` y `AvisoUtilizacionKafkaProducer.java`. La creación del aviso en la misma transacción de la Liquidación se implementa en el plan 003 (T009).

---

## Phase 4: CU09 y CU10 – Alimento y Medicamentos

- [ ] **T014** Unit Tests de `SyncModulo2Service` para partidas de alimento (valorizada, `PENDIENTE_PRECIO`, idempotencia) y consumos de medicamento (uno y varios tramos, tramo sin precio, reemplazo de tramos al reprocesar).
- [ ] **T015** Implementar `procesarPartidaAlimento` y `procesarConsumoMedicamento`.
- [ ] **T016** Integration Test `Modulo2EventConsumerIT.java` e implementación de `Modulo2EventConsumer.java`.

---

## Phase 5: Estado de la Sincronización (RT-09)

- [ ] **T017** Implementar `GET /api/v1/sincronizacion/estado`, que responde por fuente la fecha y hora de la última sincronización exitosa y `desactualizada: true` si supera `sync.umbral-inactividad` o el broker no está disponible.

```json
GET /api/v1/sincronizacion/estado

HTTP/1.1 200 OK
{
  "modulo1": { "ultimaSincronizacionExitosa": "2026-10-03T10:00:00Z", "desactualizada": false },
  "modulo2": { "ultimaSincronizacionExitosa": "2026-10-01T22:40:00Z", "desactualizada": true }
}
```
