# Feature Specification: M3-CU08 – Consultar Resultado Final de Sacrificio al Módulo 2

**Created**: 2026-09-18  
**Actualizado**: 2026-11-20 (SC-06: consulta REST en lugar de eventos Kafka)  
**Módulo**: 3 – Liquidación de Lote y Análisis de Rentabilidad (AVICONTROL)  
**Actor Externo**: Módulo 2 – Gestión Operativa del Ciclo  
**Rol Principal**: Módulo 3 (cliente automático del REST de Módulo 2)  

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sincronización del Resultado Final de Sacrificio (Priority: P1)

Como Módulo 3, quiero recibir de Módulo 2 el resultado final de sacrificio de cada lote —cantidad final de pollos y peso total en kilogramos— para disponer de la base física sobre la cual el administrador financiero genera la Liquidación, sin depender de la disponibilidad de Módulo 2 en el momento de liquidar.

**Why this priority**: Es el único insumo físico de la Venta Bruta (M3-CU03). Sin él, M3 no puede liquidar ningún lote que no sea un siniestro total.

**Independent Test**: Un servidor simulado de Módulo 2 responde con el resultado final de sacrificio de un lote y se verifica que la copia local almacena cantidad final, peso total y fecha de registro como una sola unidad de información, con su fecha y hora de sincronización.

**Acceptance Scenarios**:

1. **Scenario**: Sincronización exitosa del resultado final
   - **Given** Módulo 2 ha registrado el resultado final de sacrificio de un lote con 8.500 pollos y 23.800 kg
   - **When** M3 sincroniza el lote
   - **Then** el sistema almacena `idResultado`, `idLote`, `idGalpon`, `cantidadFinalPollos`, `pesoTotalKg`, `fechaRegistro` y la fecha y hora de sincronización, y habilita la generación de la Liquidación para ese lote

2. **Scenario**: Rechazo de un resultado inválido
   - **Given** Módulo 2 informa un resultado con cantidad final o peso total igual a cero, negativo o ausente
   - **When** M3 sincroniza el lote
   - **Then** el sistema no almacena el resultado como válido, lo registra como incidencia y mantiene bloqueada la generación de la Liquidación para ese lote

3. **Scenario**: Corrección del resultado antes de ser utilizado
   - **Given** un resultado final sincronizado que aún no ha sido consumido por ninguna Liquidación (estado `NO_UTILIZADO`) y que Módulo 2 corrigió
   - **When** M3 vuelve a sincronizar el lote con la corrección
   - **Then** el sistema actualiza la copia local con las cifras corregidas y conserva la fecha y hora de la nueva sincronización

4. **Scenario**: Aviso de utilización tras generar la Liquidación
   - **Given** una Liquidación `ACTIVA` recién generada que consume un resultado final de sacrificio
   - **When** M3 persiste la Liquidación
   - **Then** el sistema marca el resultado como utilizado en su copia local y entrega a Módulo 2 el aviso de utilización con el identificador del resultado, para que Módulo 2 pueda bloquear correcciones posteriores

5. **Scenario**: Fallo del aviso de utilización
   - **Given** M3 generó la Liquidación y el aviso de utilización a Módulo 2 no pudo entregarse
   - **When** falla la entrega
   - **Then** el sistema conserva la Liquidación generada, marca el aviso como pendiente, lo reintenta en segundo plano y NO DEBE revertir ni bloquear ninguna operación financiera ya completada

6. **Scenario**: Módulo 2 no disponible con copia local previa existente
   - **Given** existe una copia local del resultado final y Módulo 2 no está disponible
   - **When** el administrador genera la Liquidación
   - **Then** el sistema usa la copia local, conserva su fecha y hora de sincronización y no requiere comunicación en vivo

---

### Edge Cases

- **Unidad indivisible**: La cantidad final de pollos y el peso total constituyen una sola unidad de información. M3 nunca almacena ni utiliza uno sin el otro.
- **Corrección posterior al consumo**: Desde que una Liquidación consume el resultado y se emite el aviso de utilización, el resultado es **definitivo**: Módulo 2 no debe corregirlo (D-03) y, si M3 recibe una corrección, la rechaza, la registra como incidencia y conserva las cifras ya utilizadas. Anular la Liquidación no lo libera (M3-CU04): la nueva Liquidación usa el mismo resultado. Esta es la razón por la cual el aviso de utilización debe existir.
- **Un único resultado por lote**: Módulo 2 registra un solo resultado final por lote. Si M3 recibe más de uno para el mismo lote, lo registra como incidencia y conserva el previamente sincronizado.
- **Peso promedio**: M3 calcula el peso promedio a partir de estos dos valores únicamente con fines de presentación. El cálculo monetario de M3-CU03 se realiza sobre el peso total.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST consultar, mediante un proceso automático en segundo plano, el REST de Módulo 2 para obtener el resultado final de sacrificio de los lotes con ciclo cerrado (contrato acordado con M2, ver el [plan 001](../../planes/001-sincronizacion-m1-m2.md)).
- **FR-002**: El sistema MUST obtener y almacenar, como una sola unidad de información: `idResultado`, `idLote`, `idGalpon`, `cantidadFinalPollos`, `pesoTotalKg` y `fechaRegistro`.
- **FR-003**: El sistema MUST validar que `cantidadFinalPollos` sea un entero mayor que cero y que `pesoTotalKg` sea mayor que cero. Los resultados que no cumplan MUST registrarse como incidencia y NO DEBEN habilitar la generación de la Liquidación.
- **FR-004**: El sistema MUST actualizar la copia local cuando Módulo 2 corrija un resultado que aún no haya sido consumido por ninguna Liquidación (`NO_UTILIZADO`). Si el resultado ya está `UTILIZADO`, el sistema MUST rechazar la corrección, registrarla como incidencia y conservar las cifras utilizadas, aunque la Liquidación que lo consumió se haya anulado.
- **FR-005**: El sistema NO DEBE recalcular una Liquidación `ACTIVA` ante cambios posteriores en la copia local. El resultado final de sacrificio es definitivo desde su primera utilización (FR-004); la anulación formal (M3-CU04) corrige los datos propios de la Liquidación, como el precio por kg, pero no el resultado.
- **FR-006**: Al persistir una Liquidación `ACTIVA` que consume un resultado final de sacrificio, el sistema MUST marcar el resultado final asociado como utilizado en su copia local y MUST publicar el aviso de utilización con el identificador del resultado con un `POST` REST a Módulo 2, que responde `200` con `estado = UTILIZADO` (D-03). Esta es la **única publicación** de M3 hacia un módulo externo en todo el módulo. Cada resultado genera un solo aviso, aunque su Liquidación se anule y se genere otra con el mismo resultado.
- **FR-007**: El aviso de utilización MUST ser asíncrono e idempotente. Si su publicación falla, el sistema MUST conservar la Liquidación generada, mantener el aviso como pendiente y reintentarlo en segundo plano, sin revertir ni bloquear ninguna operación financiera completada.
- **FR-008**: Ante la indisponibilidad de Módulo 2 (sin respuesta, tiempo agotado o error HTTP 500), el sistema MUST conservar íntegra la copia local previa, registrar el fallo y reintentar en el siguiente ciclo, sin bloquear ninguna operación de M3.
- **FR-009**: El sistema MUST procesar las sincronizaciones de forma idempotente: recibir el mismo resultado más de una vez no DEBE crear registros duplicados.
- **FR-010**: El sistema MUST registrar como incidencia la recepción de más de un resultado final para el mismo lote, conservando el previamente sincronizado.

**D-03 (resuelta el 2026-11-21).** M2 recibe el aviso por REST (`POST /api/integracion/modulo3/resultados-sacrificio/utilizado`): responde `200` con `estado = UTILIZADO` después de confirmar su transacción, repetir el mismo `eventId` produce el mismo resultado y, desde ese momento, M2 rechaza corregir el resultado. M3 solo marca el aviso `ENTREGADO` con esa respuesta.

### Key Entities

- **ResultadoFinalSacrificio**: Réplica local del resultado operativo de Módulo 2. Atributos: `idResultado`, `idLote`, `idGalpon`, `cantidadFinalPollos`, `pesoTotalKg`, `fechaRegistro`, `fechaHoraSincronizacion`, `estadoUtilizacionLocal` (`NO_UTILIZADO` | `UTILIZADO`).
- **AvisoUtilizacionResultado**: Notificación saliente de M3 hacia Módulo 2. Atributos: `idAviso`, `idResultado`, `idLiquidacion`, `fechaHoraEmision`, `estadoEntrega` (`PENDIENTE` | `ENTREGADO`; un intento fallido mantiene `PENDIENTE`), `intentos`.
- **RegistroSincronizacion**: Bitácora técnica del proceso, compartida con M3-CU07, M3-CU09 y M3-CU10. Una fila por ejecución de la sincronización. Atributos: `idSincronizacion`, `fuente` (`MODULO_2`), `topico` (la ruta consultada), `idEvento` (vacío), `fechaHoraInicio`, `fechaHoraFin`, `resultado`, `descripcionError`, `cantidadRegistrosActualizados`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los resultados finales válidos disponibles en Módulo 2 se refleja en la copia local tras una sincronización.
- **SC-002**: 0% de Liquidaciones generadas sobre un resultado final inválido o ausente, salvo siniestro total.
- **SC-003**: El 100% de las Liquidaciones `ACTIVA` que consumen un resultado final de sacrificio genera su aviso de utilización hacia Módulo 2.
- **SC-004**: 0% de Liquidaciones revertidas o bloqueadas por un fallo en la entrega del aviso de utilización.
- **SC-005**: 0% de registros duplicados ante consultas repetidas del mismo resultado.
- **SC-006**: El 100% de las Liquidaciones conserva la fecha y hora de sincronización del resultado final utilizado.
