# Diccionario de Dominio y Datos — Módulo 3: Liquidación de Lote y Análisis de Rentabilidad (AVICONTROL)

**Versión**: 1.3  
**Fecha**: 23/10/2026  
**Módulo**: 3 – Liquidación de Lote y Análisis de Rentabilidad  
**Naturaleza**: Glosario Conceptual y Diccionario de Dominio / Base de Datos  
**DDL de referencia**: [general.md](../arquitectura/general.md#b-scripts-sql-ddl-completos-flyway-migrations)  

---

## Propósito

Este documento cumple la función dual de **Glosario Conceptual de Dominio (Domain-Driven Design)** y **Diccionario de Base de Datos Técnico**. Define los términos ubicuos del negocio utilizados en el Módulo 3 y los especifica con sus correspondientes atributos relacionales, tipos de datos SQL, llaves y restricciones de integridad. Si este documento y el DDL de la arquitectura general difieren, manda el DDL y este documento se corrige.

Todas las fechas y horas son `TIMESTAMPTZ`.

---

## 1. Entidades Principales del Dominio

### A. Tabla `liquidacion` (Entidad `Liquidacion`)
Entidad central e inmutable del módulo que consolida los resultados económicos del cierre de un lote avícola.

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_liquidacion` | `BIGSERIAL` | NO | PK | Identificador único interno autoincremental de la liquidación. |
| `id_lote` | `UUID` | NO | FK→lote | UUID del lote (M1). Índice único parcial `uq_lote_activa`: máximo una liquidación `ACTIVA` por lote; las `ANULADA` no cuentan. |
| `id_galpon` | `UUID` | NO | FK→galpon | UUID del galpón que albergó el lote, tomado de la alerta de vaciado sanitario. |
| `id_resultado_sacrificio` | `UUID` | SI | FK→resultado_final_sacrificio | Resultado de sacrificio (M2) consumido. Nulo solo en siniestro total. Varias liquidaciones (una activa y otras anuladas) pueden apuntar al mismo resultado. |
| `pollos_vendidos` | `INT` | NO | - | Cantidad final de aves comercializadas (de M2). `0` en siniestro total. |
| `peso_total_kg` | `NUMERIC(12,2)` | NO | - | Peso acumulado total en kg (M2). Base de cálculo de Venta Bruta. `0.00` en siniestro total. |
| `peso_promedio_kg` | `NUMERIC(8,4)` | SI | - | Atributo derivado `peso_total_kg / pollos_vendidos`. Informativo. Nulo en siniestro total. |
| `precio_kg_cop` | `NUMERIC(14,2)` | SI | - | Precio digitado en COP/kg por el Administrador Financiero. Nulo en siniestro total. |
| `venta_bruta_cop` | `BIGINT` | NO | - | `peso_total_kg × precio_kg_cop` con redondeo `HALF_UP` a enteros COP. `0` en siniestro total. |
| `mortalidad_aves` | `INT` | NO | - | `poblacion_inicial - poblacion_actual` (de M1). |
| `porcentaje_mortalidad` | `NUMERIC(5,2)` | NO | - | `((mortalidad_aves / poblacion_inicial) × 100)` con 2 decimales. |
| `costos_operativos_cop` | `BIGINT` | NO | - | Suma exacta de `subtotal_cop` de sus partidas (alimento + medicina + costo de población). |
| `utilidad_neta_cop` | `BIGINT` | NO | - | Ganancia o pérdida neta `venta_bruta_cop - costos_operativos_cop`. |
| `estado` | `VARCHAR(10)` | NO | CHECK | Estado del documento financiero (`ACTIVA` o `ANULADA`). |
| `fecha_hora_generacion` | `TIMESTAMPTZ` | NO | - | Fecha y hora exacta de creación del registro. |
| `usuario_responsable` | `VARCHAR(100)` | NO | - | Nombre que digitó el Administrador Financiero al generar (la plataforma no tiene login). |
| `version` | `BIGINT` | NO | - | Control de concurrencia optimista (`@Version`). No es un dato de negocio. |

---

### B. Tabla `galpon` (Entidad `Galpon` — Copia Local M1)
Reflejo local sincronizado de la infraestructura física desde Módulo 1.

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_galpon` | `UUID` | NO | PK | Identificador global único del galpón (M1). |
| `nombre` | `VARCHAR(100)` | NO | - | Código o nombre asignado (ej. "Galpón 1"). |
| `aforo_maximo` | `INT` | NO | - | Capacidad máxima de aves autorizada. |
| `estado` | `VARCHAR(30)` | NO | - | Estado operativo sincronizado (`EstadoGalpon`). |
| `fecha_hora_sync` | `TIMESTAMPTZ` | NO | - | Momento en que M3 procesó el último evento de M1 sobre este galpón. |

---

### C. Tabla `lote` (Entidad `Lote` — Copia Local M1)
Reflejo local sincronizado de la parvada alojada en un galpón desde Módulo 1. La fila se conserva aunque M1 desvincule el lote del galpón.

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_lote` | `UUID` | NO | PK | Identificador global único del lote (M1). |
| `id_galpon` | `UUID` | NO | FK→galpon | Galpón asignado en M1 (el último conocido si el lote fue desvinculado). |
| `nombre` | `VARCHAR(100)` | NO | - | Código del lote (ej. "Lote L-2026-A"). |
| `fecha_ingreso` | `DATE` | NO | - | Fecha de encasetamiento de la parvada. |
| `poblacion_inicial` | `INT` | NO | - | Cantidad inicial de aves ingresadas. |
| `poblacion_actual` | `INT` | NO | - | Aves vivas según el último evento de M1. `0` = siniestro total. |
| `costo_total_cop` | `BIGINT` | NO | - | Costo inicial de adquisición del lote de pollitos. |
| `estado` | `VARCHAR(30)` | NO | - | Estado del lote informado por M1, sincronizado tal cual. No reemplaza la etapa que M3 deriva (CU01.FR-013). |
| `fecha_hora_sync` | `TIMESTAMPTZ` | NO | - | Momento en que M3 procesó el último evento de M1 sobre este lote. |

---

### D. Tabla `alerta_vaciado_sanitario` (Entidad `AlertaVaciadoSanitario` — Copia Local M1)
Evento registrado por Módulo 1 al concluir la cosecha y desvincular el lote del galpón. Es el pivote que permite a M3 identificar qué lote está listo para liquidar (`Por Liquidar`) y cuál ya fue liquidado (`Liquidado`). Sin esta alerta, M3 no puede derivar la etapa ni habilitar la generación de la Liquidación (CU01.FR-013, CU03.FR-005).

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_alerta` | `UUID` | NO | PK | Identificador único de la alerta generado por M1. |
| `id_galpon` | `UUID` | NO | FK→galpon | Galpón que quedó en estado `VACIADO_SANITARIO`. |
| `id_lote` | `UUID` | NO | UQ, FK→lote | Lote desvinculado del galpón al cierre del ciclo productivo. |
| `fecha_hora_evento` | `TIMESTAMPTZ` | NO | - | Momento exacto en que M1 registró el vaciado sanitario. |
| `id_registro_sincronizacion` | `BIGINT` | NO | FK→registro_sincronizacion | Evento procesado que importó la alerta; su `fecha_hora_inicio` indica cuándo se recibió. |

> **Restricción**: `id_lote` es `UNIQUE`; cada lote tiene como máximo una alerta de vaciado sanitario.

> **Regla de negocio**: La alerta es inmutable una vez recibida de M1; si llega de nuevo, se ignora. Su presencia — combinada con la existencia o no de una Liquidación `ACTIVA` — determina la etapa del lote (CU01.FR-013); los lotes `Liquidado` no se listan en CU01 y se consultan en el historial (CU01.FR-014). No se elimina aunque el lote sea liquidado.

---

### E. Tabla `resultado_final_sacrificio` (Entidad `ResultadoFinalSacrificio` — Copia Local M2)
Resultado operativo del sacrificio de un lote, base física de la Venta Bruta (CU08).

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_resultado` | `UUID` | NO | PK | Identificador del resultado en M2. |
| `id_lote` | `UUID` | NO | UQ, FK→lote | Lote sacrificado. Un solo resultado por lote (CU08.FR-010). |
| `id_galpon` | `UUID` | NO | FK→galpon | Galpón del lote. |
| `cantidad_final_pollos` | `INT` | NO | - | Pollos finales (`> 0`). Unidad indivisible con `peso_total_kg`. |
| `peso_total_kg` | `NUMERIC(12,2)` | NO | - | Peso total en kg (`> 0`). |
| `fecha_registro` | `TIMESTAMPTZ` | NO | - | Momento en que M2 registró el resultado. |
| `fecha_hora_sync` | `TIMESTAMPTZ` | NO | - | Momento en que M3 procesó el último evento del resultado. |
| `estado_utilizacion` | `VARCHAR(20)` | NO | CHECK | `NO_UTILIZADO` o `UTILIZADO` (`EstadoUtilizacion`). Permanece `UTILIZADO` aunque la Liquidación se anule. |

---

### F. Tabla `partida_alimento_lote` (Entidad `PartidaAlimentoLote` — Copia Local M2)
Partida de alimento del ciclo del lote, sin valorizar (CU09).

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_partida_origen` | `UUID` | NO | PK | Identificador de la partida en M2. |
| `id_lote` | `UUID` | NO | FK→lote | Lote al que se imputa. |
| `id_galpon` | `UUID` | NO | FK→galpon | Galpón del lote. |
| `tipo_alimento` | `VARCHAR(50)` | NO | - | Pre-inicio, Inicio, Engorde, etc. |
| `cantidad_kg` | `NUMERIC(12,3)` | NO | - | Cantidad en kilogramos (criterio pendiente, D-01). |
| `precio_unitario_kg_cop` | `NUMERIC(14,2)` | SI | - | Precio neto por kg. Nulo = partida pendiente de precio. |
| `valor_impuesto_cop` | `NUMERIC(14,2)` | SI | - | Impuesto informado por M2; no se suma hasta resolver D-05. |
| `estado_valorizacion` | `VARCHAR(20)` | NO | CHECK | `VALORIZADA` o `PENDIENTE_PRECIO` (`EstadoValorizacion`). |
| `referencia_origen` | `VARCHAR(200)` | SI | - | Recepción o documento de origen en M2. |
| `fecha_hora_sync` | `TIMESTAMPTZ` | NO | - | Momento en que M3 procesó el último evento de la partida. |

---

### G. Tabla `consumo_medicamento_lote` (Entidad `ConsumoMedicamentoLote` — Copia Local M2)
Consumo de medicamento aplicado al lote (CU10).

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_consumo_origen` | `UUID` | NO | PK | Identificador del consumo en M2. |
| `id_lote` | `UUID` | NO | FK→lote | Lote tratado. |
| `id_galpon` | `UUID` | NO | FK→galpon | Galpón del lote. |
| `medicamento` | `VARCHAR(150)` | NO | - | Nombre del medicamento o vacuna. |
| `fecha_consumo` | `DATE` | NO | - | Fecha de aplicación. |
| `cantidad_unidad_base` | `NUMERIC(12,3)` | NO | - | Cantidad total en unidad base (suma de sus tramos). |
| `unidad_base` | `VARCHAR(30)` | NO | - | Unidad base tal como la informa M2 (`G`, `ML`, `UNIDAD`…). |
| `fecha_hora_sync` | `TIMESTAMPTZ` | NO | - | Momento en que M3 procesó el último evento del consumo. |

---

### H. Tabla `tramo_recepcion_consumo` (Entidad `TramoRecepcionConsumo` — Copia Local M2)
Porción de un consumo abastecida por una recepción, con su precio histórico (CU10.FR-003).

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_tramo` | `BIGSERIAL` | NO | PK | Identificador interno del tramo. |
| `id_consumo_origen` | `UUID` | NO | FK→consumo_medicamento_lote | Consumo al que pertenece. `UNIQUE (id_consumo_origen, referencia_recepcion)`. |
| `referencia_recepcion` | `VARCHAR(200)` | NO | - | Recepción de M2 que abasteció el tramo. |
| `cantidad_unidad_base` | `NUMERIC(12,3)` | NO | - | Cantidad del tramo en unidad base. |
| `precio_unitario_base_cop` | `NUMERIC(14,2)` | SI | - | Precio histórico por unidad base. Nulo = tramo pendiente de precio. |
| `valor_impuesto_cop` | `NUMERIC(14,2)` | SI | - | Impuesto informado por M2 (D-05). |
| `estado_valorizacion` | `VARCHAR(20)` | NO | CHECK | `VALORIZADA` o `PENDIENTE_PRECIO`. |

---

### I. Tabla `partida_costo_lote` (Entidad `PartidaCostoLote`)
Detalle de las partidas de gasto **congeladas** al generar una liquidación. Las reglas de llenado por categoría están en la [arquitectura general](../arquitectura/general.md#c-reglas-de-llenado-de-partida_costo_lote).

| Campo | Tipo SQL | Nulo | Llave | Descripción y Regla de Negocio |
| :--- | :--- | :--- | :--- | :--- |
| `id_partida` | `BIGSERIAL` | NO | PK | Identificador único autoincremental de la partida. |
| `id_liquidacion` | `BIGINT` | NO | FK→liquidacion | Liquidación asociada. |
| `id_lote` | `UUID` | NO | FK→lote | Lote evaluado. |
| `categoria` | `VARCHAR(20)` | NO | CHECK | `ALIMENTO`, `MEDICINA` o `POBLACION` (`CategoriaCosto`). |
| `concepto` | `VARCHAR(200)` | NO | - | Nombre del ítem (ej. "Engorde", "Amoxicilina", "Costo de adquisición del lote (9000 aves)"). |
| `cantidad` | `NUMERIC(12,3)` | NO | - | Cantidad en la unidad de medida. |
| `unidad_medida` | `VARCHAR(30)` | NO | - | `KG` (alimento), unidad base de M2 (medicina) o `LOTE` (población). |
| `precio_unitario_cop` | `NUMERIC(14,2)` | NO | - | Precio unitario aplicado. |
| `subtotal_cop` | `BIGINT` | NO | - | `HALF_UP(cantidad × precio_unitario_cop)`; en `POBLACION` es exactamente `costo_total_cop`. |
| `fuente_origen` | `VARCHAR(20)` | NO | CHECK | Módulo emisor (`MODULO_1`, `MODULO_2`). |
| `referencia_origen` | `VARCHAR(200)` | SI | - | UUID o código de transacción origen. |

---

## 2. Entidades Secundarias y de Auditoría

### A. Tabla `registro_anulacion` (Entidad `RegistroAnulacion`)
Auditoría inalterable de cancelaciones de liquidaciones activas.

| Campo | Tipo SQL | Nulo | Llave | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `id_anulacion` | `BIGSERIAL` | NO | PK | Identificador único de la anulación. |
| `id_liquidacion` | `BIGINT` | NO | UQ/FK | Referencia a la liquidación anulada (1 a 1). |
| `motivo` | `VARCHAR(500)` | NO | - | Justificación escrita del usuario (10 a 500 caracteres). |
| `fecha_hora_anulacion` | `TIMESTAMPTZ` | NO | - | Estampa de tiempo exacta del evento. |
| `usuario_responsable` | `VARCHAR(100)` | NO | - | Nombre que digitó quien anuló. |

---

### B. Tabla `registro_sincronizacion` (Entidad `RegistroSincronizacion`)
Bitácora de los eventos Kafka procesados desde Módulo 1 y Módulo 2. Una fila por evento.

| Campo | Tipo SQL | Nulo | Llave | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | NO | PK | Identificador único del registro. |
| `fuente` | `VARCHAR(20)` | NO | CHECK | Módulo emisor (`MODULO_1`, `MODULO_2`). |
| `topico` | `VARCHAR(100)` | NO | - | Tópico Kafka del que se leyó el evento. |
| `id_evento` | `UUID` | NO | UQ | `eventId` del sobre; garantiza que un evento se procese una sola vez. Un mensaje ilegible o sin `eventId` no crea fila: pasa directo a la DLT. |
| `fecha_hora_inicio` | `TIMESTAMPTZ` | NO | - | Inicio del procesamiento. |
| `fecha_hora_fin` | `TIMESTAMPTZ` | SI | - | Fin del procesamiento. Nulo mientras el evento está `PENDIENTE_LOTE`; al aplicarse toma la hora en que se aplicó. |
| `resultado` | `VARCHAR(20)` | SI | CHECK | `EXITOSA`, `FALLIDA` o `PENDIENTE_LOTE` (`ResultadoSincronizacion`). |
| `descripcion_error` | `TEXT` | SI | - | Causa del rechazo o del fallo, si ocurrió. |
| `registros_actualizados` | `INT` | NO | - | Filas creadas o modificadas (Default: 0). |

---

### C. Tabla `snapshot_datos_origen` (Entidad `SnapshotDatosOrigen`)
Captura inmutable, por fuente, de los datos usados al liquidar. Una fila por fuente (`UNIQUE (id_liquidacion, fuente)`).

| Campo | Tipo SQL | Nulo | Llave | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | NO | PK | Identificador de la foto. |
| `id_liquidacion` | `BIGINT` | NO | FK→liquidacion | Liquidación asociada. |
| `id_lote` | `UUID` | NO | FK→lote | Lote liquidado. |
| `fuente` | `VARCHAR(20)` | NO | - | `MODULO_1` o `MODULO_2`. |
| `fecha_hora_sync` | `TIMESTAMPTZ` | NO | - | Hora de la sincronización usada (M1: la del lote; M2: la más reciente entre el resultado, las partidas de alimento y los consumos de medicamento del lote). |
| `poblacion_inicial` | `INT` | SI | - | Población inicial congelada (solo `MODULO_1`). |
| `poblacion_actual` | `INT` | SI | - | Población viva congelada (solo `MODULO_1`). |
| `costo_total_lote_cop` | `BIGINT` | SI | - | Costo inicial del lote congelado (solo `MODULO_1`). |

---

### D. Tabla `evento_pendiente_lote` (Entidad `EventoPendienteLote`)
Eventos de Módulo 1 o Módulo 2 que llegaron antes que su lote (Kafka no ordena entre tópicos). Se aplican y se borran cuando el lote llega por `avicontrol.m1.galpones`.

| Campo | Tipo SQL | Nulo | Llave | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `id_evento` | `UUID` | NO | PK, FK→registro_sincronizacion(id_evento) | Evento retenido; su fila de la bitácora está `PENDIENTE_LOTE`. |
| `id_lote` | `UUID` | NO | - | Lote esperado. Sin FK porque el lote aún no existe. |
| `topico` | `VARCHAR(100)` | NO | - | Tópico de origen; decide cómo se aplica el evento. |
| `payload` | `JSONB` | NO | - | Datos del evento ya validados. |
| `fecha_hora_recepcion` | `TIMESTAMPTZ` | NO | - | Momento de llegada; los pendientes se aplican en este orden. |

---

### E. Tabla `aviso_utilizacion_resultado` (Entidad `AvisoUtilizacionResultado`)
Aviso saliente hacia Módulo 2 (tópico `avicontrol.m3.avisos-utilizacion`) indicando que el resultado de sacrificio fue consumido. Funciona como *outbox*.

| Campo | Tipo SQL | Nulo | Llave | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `id_aviso` | `BIGSERIAL` | NO | PK | Identificador del aviso saliente. |
| `id_resultado` | `UUID` | NO | UQ, FK→resultado_final_sacrificio | Resultado avisado. Un solo aviso por resultado. |
| `id_liquidacion` | `BIGINT` | NO | FK→liquidacion | Primera liquidación que consumió el resultado. |
| `fecha_hora_emision` | `TIMESTAMPTZ` | NO | - | Momento en que se creó el aviso. |
| `estado_entrega` | `VARCHAR(20)` | NO | CHECK | `PENDIENTE` o `ENTREGADO` (`EstadoEntregaAviso`). Un intento fallido mantiene `PENDIENTE`. |
| `intentos` | `INT` | NO | - | Intentos de publicación acumulados. |

---

## 3. Catálogos y Enumeraciones del Sistema

Los códigos se almacenan y exponen en mayúsculas con guion bajo. La traducción desde la grafía de M1 se hace en el consumidor Kafka (D-07).

### `EstadoGalpon` (Enum)
- **`DISPONIBLE`**: Galpón sanitizado y listo para encasetar, sin lote.
- **`VACIADO_SANITARIO`**: Aves retiradas; sanitización en curso. M1 emite la alerta de vaciado sanitario al entrar en este estado. Lo que habilita la liquidación es la **alerta**, no el estado actual del galpón (que puede volver a ocuparse).
- **`PRODUCTIVO`**: Aves en crecimiento activo.
- **`EN_COSECHA`**: Despacho de pollos en progreso, a cargo de M2.
- **`MANTENIMIENTO`**: Reparaciones locativas, sin lote.
- **`AISLAMIENTO`**: Medida biosanitaria preventiva.

### `EtapaLote` (Enum, derivado por M3; no se almacena)
- **`PRODUCTIVO`**, **`EN_COSECHA`**, **`AISLAMIENTO`**: según el estado del galpón vinculado.
- **`POR_LIQUIDAR`**: tiene alerta de vaciado sanitario y no tiene Liquidación `ACTIVA`.
- **`LIQUIDADO`**: tiene alerta y Liquidación `ACTIVA`. No se lista en CU01.

### `EstadoLiquidacion` (Enum)
- **`ACTIVA`**: Documento financiero oficial e inmutable.
- **`ANULADA`**: Cancelada formalmente por el usuario.

### `EstadoUtilizacion` (Enum)
- **`NO_UTILIZADO`**: Resultado de sacrificio aún no consumido por ninguna liquidación.
- **`UTILIZADO`**: Resultado consumido por una liquidación. Es definitivo: se mantiene aunque la liquidación se anule y M3 rechaza las correcciones de M2 (CU08.FR-004).

### `EstadoValorizacion` (Enum)
- **`VALORIZADA`**: La partida o tramo tiene precio unitario.
- **`PENDIENTE_PRECIO`**: Sin precio; bloquea la liquidación (CU03.FR-006).

### `EstadoEntregaAviso` (Enum)
- **`PENDIENTE`**: Aún no confirmado por el broker; se reintenta.
- **`ENTREGADO`**: Publicado y confirmado.

### `CategoriaCosto` (Enum)
- **`ALIMENTO`**: Concentrados y suplementos (M2). Etiqueta: "Alimento".
- **`MEDICINA`**: Vacunas y tratamientos (M2). Etiqueta: "Insumos Médicos".
- **`POBLACION`**: Adquisición inicial de la parvada (M1). Etiqueta: "Costo de Población".

### `FuenteSincronizacion` (Enum)
- **`MODULO_1`**: Sistema de Galpones y Parvadas.
- **`MODULO_2`**: Sistema de Operaciones, Sacrificio e Insumos.

### `ResultadoSincronizacion` (Enum)
- **`EXITOSA`**: Evento procesado correctamente.
- **`FALLIDA`**: Evento rechazado por contenido inválido.
- **`PENDIENTE_LOTE`**: Evento válido cuyo lote aún no está en la copia local; espera en `evento_pendiente_lote` y pasa a `EXITOSA` o `FALLIDA` cuando llega el lote.

### `CausaNoLiquidable` (Enum, campo `causa` del error `422`)
- **`SIN_ALERTA_VACIADO`**, **`SIN_RESULTADO_SACRIFICIO`**, **`SIN_PARTIDAS_ALIMENTO`**, **`PARTIDAS_SIN_PRECIO`**.
