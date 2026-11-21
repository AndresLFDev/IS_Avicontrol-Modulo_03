# Feature Specification: M3-CU07 – Consultar Galpón y Lote al Módulo 1

**Created**: 2026-09-18  
**Actualizado**: 2026-11-20 (SC-06: consulta REST en lugar de eventos Kafka)  
**Módulo**: 3 – Liquidación de Lote y Análisis de Rentabilidad (AVICONTROL)  
**Actor Externo**: Módulo 1 – Gestión de Galpones y Lotes  
**Rol Principal**: Módulo 3 (cliente automático del REST de Módulo 1)  

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sincronización de Galpones, Lotes y Alertas de Vaciado (Priority: P1)

Como Módulo 3, quiero consultar periódicamente el REST de Módulo 1 sobre el estado de los galpones, los datos de sus lotes —incluidos población inicial, población actual y costo total— y los lotes ya desvinculados por vaciado sanitario, para mantener una copia local que sostenga todas mis operaciones financieras sin depender de la disponibilidad de Módulo 1 en el momento de operar.

**Why this priority**: Es el insumo de los tres cálculos que Módulo 1 alimenta en la Liquidación: la mortalidad del lote, el Costo de Población y la precondición de vaciado sanitario que habilita liquidar. Sin esta sincronización, M3-CU01 no tiene qué listar y M3-CU03 no puede calcular.

**Independent Test**: Un servidor simulado de Módulo 1 responde con los JSON de su acuerdo y se verifica que la copia local queda poblada con los galpones, sus lotes activos, las alertas de vaciado sanitario derivadas y la fecha y hora de la sincronización. Se responde un dato inválido y se detiene el servidor simulado, y se comprueba que M3 conserva la copia anterior, registra el fallo y reanuda la consulta en el siguiente ciclo sin bloquear ninguna operación.

**Acceptance Scenarios**:

1. **Scenario**: Sincronización inicial exitosa
   - **Given** Módulo 1 responde con sus galpones y M3 no tiene copia local previa
   - **When** M3 ejecuta la sincronización
   - **Then** el sistema almacena por cada galpón su UUID, nombre, aforo máximo y estado operativo; por cada lote activo su UUID, nombre, fecha de ingreso, población inicial, población actual y costo total en COP; y registra la fecha y hora del procesamiento

2. **Scenario**: Sincronización incremental con cambios de población
   - **Given** existe una copia local previa y Módulo 1 ha actualizado la población actual de un lote por mortalidad reportada desde Módulo 2
   - **When** M3 ejecuta la siguiente sincronización
   - **Then** el sistema actualiza la población actual en la copia local, conserva la población inicial sin alteración y actualiza la fecha y hora de sincronización

3. **Scenario**: Sincronización de alertas de vaciado sanitario
   - **Given** Módulo 1 ha aceptado un vaciado sanitario y desvinculado el lote de su galpón, de modo que el lote aparece en `lotesAnteriores` del galpón
   - **When** M3 ejecuta la sincronización
   - **Then** el sistema almacena el lote como desvinculado y crea su alerta de vaciado con `idAlerta` propio (determinista), `idGalpon`, `idLote` y `fechaHoraEvento` igual al momento en que la detectó, y conserva los datos del lote desvinculado para que siga siendo identificable y liquidable

4. **Scenario**: Módulo 1 no está disponible
   - **Given** existe una copia local sincronizada y Módulo 1 no responde o responde con error
   - **When** el usuario opera en M3
   - **Then** el sistema conserva íntegra la copia local anterior, registra la indisponibilidad con su fecha y hora, no marca los datos como inválidos, muestra la antigüedad de la copia (RT-09) y reanuda la consulta en el siguiente ciclo cuando Módulo 1 vuelve, sin bloquear ninguna operación de M3

5. **Scenario**: Sin copia local previa
   - **Given** M3 no ha sincronizado todavía ningún dato de Módulo 1 para un galpón o lote
   - **When** el usuario intenta operar sobre ese lote
   - **Then** el sistema bloquea la operación e informa que no hay datos sincronizados disponibles para ese lote

6. **Scenario**: Sincronización idempotente
   - **Given** una alerta de vaciado sanitario ya almacenada en la copia local
   - **When** M3 vuelve a consultar y el lote sigue en `lotesAnteriores`
   - **Then** el sistema no crea un registro duplicado ni modifica la alerta almacenada, que es inmutable

---

### Edge Cases

- **Población actual mayor a cero en `Vaciado Sanitario`**: Módulo 1 no exige población cero para transicionar a `Vaciado sanitario`. La diferencia entre población inicial y población actual representa aves muertas, no aves vendidas. M3 almacena ambos valores tal como los informa Módulo 1 y no los interpreta durante la sincronización.
- **Costo total inmutable**: El costo total del lote se captura en Módulo 1 al registrarlo y es un entero positivo en COP. M3 lo almacena sin transformarlo y lo usa como Costo de Población en M3-CU03.
- **Datos parciales**: Si Módulo 1 responde con un galpón sin lote activo, M3 almacena el galpón y deja vacíos los campos de lote. No se infiere ni se completa ningún valor ausente.
- **Contrato con Módulo 1**: Módulo 1 publica en su repositorio un [acuerdo de contratos](https://github.com/AviControl-org/AVICONTROL/blob/develop/avicontrol/docs/plan-general/acuerdo-contratos-entre-modulos.md) que declara tres consultas de solo lectura para Módulo 3: `GET /api/v1/galpones`, `GET /api/v1/galpones/{id}` y `GET /api/v1/galpones/{id}/lotes`. M3 usa esos nombres de campo sin traducirlos en el contrato (`capacidad`, `loteActivo`, `costoTotal`) y los traduce a los suyos en el cliente REST. Módulo 1 no publica eventos hacia Módulo 3.
- **Alerta derivada**: Módulo 1 no expone el `alertaId` ni la hora del vaciado. Un lote deja de ser activo solo cuando Módulo 1 acepta el vaciado sanitario, así que aparecer en `lotesAnteriores` equivale a tener la alerta. M3 deriva la alerta con un id propio y la hora en que la detectó.
- **Un galpón sin lote activo** responde `200` sin el campo `loteActivo` (nunca `null`); `mensaje` e `incidencias` son informativos.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST consultar, mediante un proceso automático en segundo plano que corre al arrancar y cada intervalo configurable, el REST de Módulo 1 (`GET /galpones`, `GET /galpones/{id}` y `GET /galpones/{id}/lotes`) para obtener los datos de galpones y lotes, y MUST derivar de ellos las alertas de vaciado sanitario.
- **FR-002**: Por cada galpón, el sistema MUST obtener y almacenar: `id` (como `idGalpon`), `nombre`, `capacidad` (como `aforoMaximo`) y `estado` operativo.
- **FR-003**: Por cada lote, activo o ya desvinculado, el sistema MUST obtener y almacenar: `id` (como `idLote`), `idGalpon`, `nombre`, `fechaIngreso`, `poblacionInicial`, `poblacionActual` y `costoTotal` (como `costoTotalCop`).
- **FR-004**: Por cada lote que aparezca en `lotesAnteriores`, el sistema MUST crear una sola vez su alerta de vaciado sanitario con `idAlerta` propio, `idGalpon`, `idLote` y `fechaHoraEvento` (momento de detección), conservándola aun después de que el lote deje de aparecer en las consultas.
- **FR-005**: El sistema MUST registrar, por cada ejecución de la sincronización, su fecha y hora y su resultado (exitosa o fallida), y MUST exponer la fecha y hora de la última sincronización exitosa a las especificaciones que consumen la copia local.
- **FR-006**: Ante la indisponibilidad de Módulo 1 (sin respuesta, tiempo agotado o error HTTP 500), el sistema MUST conservar íntegra la copia local previa, registrar el fallo y reintentar en el siguiente ciclo, sin bloquear ninguna operación de M3 ni afectar a los demás módulos.
- **FR-007**: El sistema MUST procesar las sincronizaciones de forma idempotente: consultar el mismo dato más de una vez no DEBE crear registros duplicados.
- **FR-008**: El sistema NO DEBE escribir, modificar ni eliminar información en Módulo 1 bajo ninguna circunstancia. La relación con Módulo 1 es exclusivamente de lectura.
- **FR-009**: El sistema NO DEBE transformar, redondear ni interpretar los valores obtenidos. La población inicial, la población actual y el costo total se almacenan tal como los informa Módulo 1.
- **FR-010**: El sistema MUST rechazar y registrar como incidencia los datos que lleguen incompletos o inconsistentes (lote sin población inicial, costo total no positivo, población actual mayor que la inicial), sin sobrescribir con ellos la copia local válida previa y sin reintentar su procesamiento.
- **FR-011**: El sistema MUST admitir el catálogo de estados operativos definido en M3-CU01.FR-003 y MUST registrar como incidencia cualquier estado recibido que no pertenezca a ese catálogo.

[RESUELTO – D-02 y D-08 para Módulo 1: el acuerdo de contratos de Módulo 1 declara las tres consultas REST que usa esta especificación. Queda pendiente solo que Módulo 1 marque como acordados los puntos de Módulo 3 en su lista de verificación.]

[NEEDS CLARIFICATION – D-04: Módulo 1 acepta alertas de vaciado sanitario únicamente cuando el galpón está en estado `En cosecha`, y no inicia el vaciado cuando la población actual llega a cero. Un lote con mortalidad total en estado `Productivo` no alcanza el estado que habilita la Liquidación en M3-CU03. Ruta de estado pendiente de acuerdo.]

[NEEDS CLARIFICATION – D-06: Módulo 1 acepta alertas de mortalidad únicamente cuando el galpón está en estado `Productivo`. La mortalidad ocurrida durante `En cosecha` o `Aislamiento` no reduce la población actual, lo que subestima la mortalidad calculada en M3-CU03. Tratamiento pendiente de acuerdo.]

### Key Entities

- **CopiaLocalGalponLote**: Réplica local de los galpones y sus lotes (activos y desvinculados), mantenida por esta especificación. Atributos: los definidos en FR-002 y FR-003, más `fechaHoraUltimaSincronizacion` y `estadoUltimaSincronizacion`.
- **AlertaVaciadoSanitario**: Réplica local de la alerta aceptada por Módulo 1. Atributos: `idAlerta`, `idGalpon`, `idLote`, `fechaHoraEvento`, `idRegistroSincronizacion` (ejecución que la creó).
- **RegistroSincronizacion**: Bitácora técnica, una fila por ejecución de la sincronización. Atributos: `idSincronizacion`, `fuente` (`MODULO_1`), `topico` (la ruta consultada), `idEvento` (vacío), `fechaHoraInicio`, `fechaHoraFin`, `resultado`, `descripcionError`, `cantidadRegistrosActualizados`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los galpones, lotes y alertas de vaciado sanitario disponibles en Módulo 1 se refleja en la copia local tras una sincronización.
- **SC-002**: El 100% de las operaciones de M3 se resuelve con la copia local, sin comunicación en vivo con Módulo 1.
- **SC-003**: 0% de pérdidas de la copia local previa ante fallos de Módulo 1.
- **SC-004**: 0% de registros duplicados ante consultas repetidas del mismo dato.
- **SC-005**: 0% de escrituras de M3 sobre Módulo 1.
- **SC-006**: El 100% de las ejecuciones de la sincronización queda registrado con fecha, hora y resultado.
