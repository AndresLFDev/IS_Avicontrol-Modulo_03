### Test Case Specification: Anular liquidación
**Created** : 2026-11-13  **Spec/Plan Link** : [spec.md](../../features/m3-cu04-anular-liquidacion/spec.md) · [003-gestion-liquidaciones.md](../../planes/003-gestion-liquidaciones.md)  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-M3-CU04-AnularLiquidacion
**Product Name & Version** : AviControl Módulo 3 (rama `develop`, sin versión entregada)
**Author** : Juan Grau
**Current Owner** : Equipo Módulo 3
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that **la anulación de una liquidación activa** (`PUT /api/v1/liquidaciones/{id}/anulacion`, desde el botón "Anular liquidación") functions successfully under specified operational conditions, y que el sistema:

- exige un motivo de 10 a 500 caracteres y un usuario responsable de 3 a 100 caracteres;
- cambia el estado a `ANULADA` conservando todos los valores de la liquidación, y guarda el registro de anulación con el motivo, el usuario y la fecha;
- devuelve el lote a la lista de lotes por liquidar;
- rechaza anular una liquidación ya anulada y muestra la anulación existente;
- permite generar una liquidación nueva del mismo lote después de anular, sin duplicar el aviso de utilización.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| M3-CU04 Anular liquidación | Specification Document | [features/m3-cu04-anular-liquidacion/spec.md](../../features/m3-cu04-anular-liquidacion/spec.md) |
| Plan 003 Gestión de liquidaciones | Plan Document | [planes/003-gestion-liquidaciones.md](../../planes/003-gestion-liquidaciones.md) |
| Guía de pruebas manuales | Process Document | [proceso/guia-pruebas-manuales.md](../../proceso/guia-pruebas-manuales.md) |
| Consultas de apoyo | SQL | Al final de este caso: consultas 1, 6, 10, 11, 12b y 12c. También reunidas en [datos/consultas-demo.sql](../datos/consultas-demo.sql) |
| Reinicio de datos | SQL | Al final de este caso. El mismo archivo está en [datos/reset-demo.sql](../datos/reset-demo.sql) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* No hay roles ni inicio de sesión: cualquier persona que abra la liquidación puede anularla. El usuario responsable es texto libre.
* La anulación de dos usuarios al mismo tiempo no se puede provocar a mano de forma confiable (ver EC-002).

##### Dependencies & Assumptions
* Entorno preparado según la [guía de pruebas manuales](../../proceso/guia-pruebas-manuales.md), sección 2.
* La Sección 1 del caso [TC-M3-CU03](m3-cu03-generar-liquidacion.md) pasó: existe una liquidación `ACTIVA` de L-2026-03. Este caso la usa como punto de partida.

##### Preconditions & Default Setup
* **Initial State** : base con el seed `V1000` y una liquidación `ACTIVA` de L-2026-03 con precio 4500 (venta 107.100.000, utilidad 22.100.000). Si hay otros datos, ejecutar la consulta de reinicio (al final de este caso) y generarla de nuevo.
* **Environment** : local. PostgreSQL en Docker, backend con perfil `dev`, frontend con `npm run dev:backend`.
* **Test Accounts** : no aplica. Usuario responsable en los pasos: `Juan Grau`.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

**Última ejecución** : 2026-11-13. Los resultados del backend y de la base se verificaron contra la app real (`bootRun` con perfil `dev` y el seed), llamando a la API con los mismos datos y consultando con `psql`. Lo que muestra la pantalla se leyó en el código del frontend y no se vio en el navegador: esos pasos quedan en `[~]`.

##### Section 1: Anular la liquidación activa
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Abrir la anulación | Abrir la liquidación activa de L-2026-03 y pulsar "Anular liquidación" | N/A | Se abre la ventana "Anular liquidación" con los campos "Motivo de anulación" y "Usuario responsable" | [~] | Texto tomado del código |
| 2 | Motivo demasiado corto | Escribir el motivo y pulsar para continuar | Motivo `Error` | No deja continuar y avisa "Escribe al menos 10 caracteres (faltan 5)." | [~] | La validación está en la pantalla. En la API, el mismo motivo da 400 `validacion`: "El motivo de anulación debe tener entre 10 y 500 caracteres." |
| 3 | Usuario demasiado corto | Escribir un usuario de 2 caracteres | Usuario `Ju` | Avisa "Escribe al menos 3 caracteres." | [~] | Texto tomado del código |
| 4 | Continuar con datos válidos | Escribir el motivo y el usuario y continuar | Motivo `Precio por kg mal digitado`, usuario `Juan Grau` | Pasa a la confirmación con el título "¿Anular la liquidación de este lote?" | [~] | Texto tomado del código |
| 5 | Confirmar | Pulsar el botón de confirmar la anulación | N/A | La liquidación queda en estado Anulada con todos sus valores (venta 107.100.000, utilidad 22.100.000). Ya no tiene el botón "Anular liquidación" y aparece "Generar nueva liquidación" | [~] | API: responde 200 con `estado` `ANULADA` y el `registroAnulacion` (motivo, usuario, fecha). La liquidación consultada conserva venta 107.100.000 y utilidad 22.100.000 |
| 6 | Revisar la liquidación | Ejecutar la consulta 6 (al final de este caso) | N/A | El estado pasó a `ANULADA` y los valores no cambiaron | [✔] | Verificado con la base |
| 7 | Revisar el registro de anulación | Ejecutar la consulta 11 (al final de este caso) | N/A | Una fila con el motivo `Precio por kg mal digitado`, el usuario `Juan Grau` y la fecha | [✔] | Verificado con la base |
| 8 | Comprobar la consistencia | Ejecutar la consulta 12c (al final de este caso) | N/A | `coherente` en `t`: la anulada tiene un registro de anulación | [✔] | Verificado con la base |
| 9 | Ver la lista de lotes | Abrir <http://localhost:5173/> | N/A | L-2026-03 vuelve a aparecer como `Por Liquidar` | [~] | API: la lista vuelve a tener 6 lotes con L-2026-03 |

--------------------------------------------------------------------------------

##### Section 2: Anular una liquidación ya anulada
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar una liquidación activa | Generar una liquidación nueva (Sección 3, pasos 1 a 3) | Precio `4600` | Liquidación `ACTIVA` | [~] | Verificado con la API |
| 2 | Abrirla en dos pestañas | Abrir la misma liquidación en dos pestañas del navegador | N/A | En las dos aparece el botón "Anular liquidación" | [ ] | Pendiente de ver en pantalla |
| 3 | Anular en la primera pestaña | Seguir la Sección 1, pasos 3 a 5 | Motivo `Precio por kg mal digitado` | Queda `ANULADA` | [~] | Verificado con la API |
| 4 | Anular en la segunda pestaña | En la segunda pestaña, que aún muestra el botón, intentar anular | Motivo `Precio por kg mal digitado` | 409 `liquidacion-ya-anulada`: "La Liquidación <id> ya fue anulada." con el registro de la anulación existente. La pantalla termina el flujo y muestra ese mensaje | [~] | API verificada: 409 con el `registroAnulacion` original. La pantalla trata este error de forma propia (`onTerminar`); el texto exacto queda por ver |
| 5 | Confirmar que sigue una sola anulación | Ejecutar la consulta 11 (al final de este caso) | N/A | Una sola anulación por liquidación (la columna `id_liquidacion` es única) | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Section 3: Generar una liquidación nueva después de anular
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Entrar al flujo de generación | Desde la liquidación anulada pulsar "Generar nueva liquidación", o abrir L-2026-03 desde la lista | N/A | Se abre la ventana "Generar liquidación" del lote | [~] | Texto tomado del código |
| 2 | Pedir la vista previa con otro precio | Completar los campos y pulsar "Siguiente" | Precio `4600`, usuario `Juan Grau` | Venta bruta $109.480.000 (23.800 kg por 4.600), utilidad neta $24.480.000. Los costos siguen en $85.000.000 | [~] | API: venta 109.480.000, costos 85.000.000, utilidad 24.480.000 |
| 3 | Generar | Pulsar "Generar liquidación" | N/A | Nueva liquidación `ACTIVA` | [~] | API: 201 |
| 4 | Revisar las liquidaciones del lote | Ejecutar las consultas 6 y 12b (al final de este caso) | N/A | Dos filas de L-2026-03: una `ANULADA` con precio 4.500 y una `ACTIVA` con 4.600. `activas` igual a 1 y `anuladas` igual a 1 | [✔] | Verificado con la base |
| 5 | Revisar el aviso de utilización | Ejecutar la consulta 10 (al final de este caso) | N/A | Una sola fila para L-2026-03, con `liquidacion_del_aviso` apuntando a la primera liquidación. No se duplica | [✔] | Verificado con la base. También lo cubre `trasAnularSePuedeReliquidarYElAvisoNoSeDuplica` |
| 6 | Dejar la base limpia | Ejecutar la consulta de reinicio (al final de este caso) | N/A | Sin liquidaciones; la consulta 1 vuelve a mostrar los tres lotes `POR_LIQUIDAR` | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Consultas SQL de este caso
Las consultas de apoyo y el reinicio están aquí mismo, listos para copiar; no hace falta abrir ningún otro archivo. La numeración es la de [`datos/consultas-demo.sql`](../datos/consultas-demo.sql), que las reúne todas. Conexión de desarrollo: host `localhost`, puerto `DB_PORT` del `.env` (5433), base `avicontrol`, usuario y contraseña `avicontrol`. En una consola SQL, pon el cursor dentro de la consulta y pulsa Ctrl+Enter. Las consultas son de solo lectura; solo la de reinicio borra datos.

**Consulta 1**

```sql
-- 1. Lotes con su galpón y la etapa que calcula la app
-- Muestra los 6 lotes del seed, el estado del galpón y la etapa (la que ve la pantalla
-- "Lista de lotes"). La etapa sale de la alerta de vaciado y del estado del galpón, no del
-- estado del lote. Un lote con liquidación ACTIVA pasa a LIQUIDADO y la app lo oculta.
-- Esperado al inicio: L-2026-01 PRODUCTIVO, 02 EN_COSECHA, 03 POR_LIQUIDAR,
-- 04 POR_LIQUIDAR, 05 AISLAMIENTO, 06 POR_LIQUIDAR.
SELECT l.nombre              AS lote,
       g.nombre              AS galpon,
       g.estado              AS estado_galpon,
       l.poblacion_inicial,
       l.poblacion_actual,
       CASE
         WHEN a.id_alerta IS NOT NULL AND liq.id_liquidacion IS NOT NULL THEN 'LIQUIDADO'
         WHEN a.id_alerta IS NOT NULL THEN 'POR_LIQUIDAR'
         WHEN g.estado = 'PRODUCTIVO' THEN 'PRODUCTIVO'
         WHEN g.estado = 'EN_COSECHA' THEN 'EN_COSECHA'
         WHEN g.estado = 'AISLAMIENTO' THEN 'AISLAMIENTO'
       END                   AS etapa
  FROM lote l
  JOIN galpon g ON g.id_galpon = l.id_galpon
  LEFT JOIN alerta_vaciado_sanitario a ON a.id_lote = l.id_lote
  LEFT JOIN liquidacion liq ON liq.id_lote = l.id_lote AND liq.estado = 'ACTIVA'
 ORDER BY l.nombre;
```

**Consulta 6**

```sql
-- 6. Liquidaciones generadas
-- Una fila por liquidación. Mira estado (ACTIVA o ANULADA), el precio, la venta bruta, la
-- mortalidad, los costos y la utilidad. En siniestro total, precio_kg_cop y peso_promedio_kg
-- son NULL, pollos_vendidos es 0 y id_resultado_sacrificio es NULL.
-- Esperado para L-2026-03 con precio 4500: venta 107.100.000, costos 85.000.000,
-- utilidad 22.100.000, mortalidad 500 aves (5,56 %).
-- Esperado para L-2026-04: venta 0, costos 32.800.000, utilidad -32.800.000, mortalidad 100 %.
SELECT liq.id_liquidacion, l.nombre AS lote, liq.estado, liq.precio_kg_cop,
       liq.pollos_vendidos, liq.peso_total_kg, liq.venta_bruta_cop,
       liq.mortalidad_aves, liq.porcentaje_mortalidad,
       liq.costos_operativos_cop, liq.utilidad_neta_cop,
       liq.usuario_responsable, liq.fecha_hora_generacion
  FROM liquidacion liq
  JOIN lote l ON l.id_lote = liq.id_lote
 ORDER BY liq.id_liquidacion;
```

**Consulta 10**

```sql
-- 10. Aviso de utilización para M2
-- Al generar una liquidación con resultado de sacrificio, el resultado pasa a UTILIZADO y se
-- crea un aviso PENDIENTE (M2 lo recibiría por Kafka; ese envío aún no está implementado).
-- Si se anula y se vuelve a liquidar, el aviso original se conserva y no se duplica.
-- En siniestro total no hay resultado, así que no se crea aviso.
SELECT r.id_resultado, l.nombre AS lote, r.estado_utilizacion,
       a.id_liquidacion AS liquidacion_del_aviso, a.estado_entrega, a.intentos
  FROM resultado_final_sacrificio r
  JOIN lote l ON l.id_lote = r.id_lote
  LEFT JOIN aviso_utilizacion_resultado a ON a.id_resultado = r.id_resultado
 ORDER BY l.nombre;
```

**Consulta 11**

```sql
-- 11. Anulaciones registradas
-- Cada anulación guarda el motivo (10 a 500 caracteres), quién anuló y cuándo. La liquidación
-- anulada se conserva con todos sus valores; solo cambia su estado a ANULADA.
SELECT ra.id_anulacion, ra.id_liquidacion, l.nombre AS lote, ra.motivo,
       ra.usuario_responsable, ra.fecha_hora_anulacion
  FROM registro_anulacion ra
  JOIN liquidacion liq ON liq.id_liquidacion = ra.id_liquidacion
  JOIN lote l ON l.id_lote = liq.id_lote
 ORDER BY ra.id_anulacion;
```

**Consulta 12b**

```sql
-- 12b. Nunca debe haber más de una liquidación ACTIVA por lote (lo garantiza el índice
--      uq_lote_activa). La columna activas debe ser 0 o 1 en todas las filas.
SELECT l.nombre AS lote,
       COUNT(*) FILTER (WHERE liq.estado = 'ACTIVA')  AS activas,
       COUNT(*) FILTER (WHERE liq.estado = 'ANULADA') AS anuladas
  FROM liquidacion liq
  JOIN lote l ON l.id_lote = liq.id_lote
 GROUP BY l.nombre
 ORDER BY l.nombre;
```

**Consulta 12c**

```sql
-- 12c. Toda liquidación ANULADA debe tener exactamente un registro de anulación, y ninguna
--      ACTIVA debe tenerlo. La columna coherente debe dar true en todas las filas.
SELECT liq.id_liquidacion, liq.estado,
       COUNT(ra.id_anulacion) AS registros_anulacion,
       (liq.estado = 'ANULADA' AND COUNT(ra.id_anulacion) = 1)
         OR (liq.estado = 'ACTIVA' AND COUNT(ra.id_anulacion) = 0) AS coherente
  FROM liquidacion liq
  LEFT JOIN registro_anulacion ra ON ra.id_liquidacion = liq.id_liquidacion
 GROUP BY liq.id_liquidacion, liq.estado
 ORDER BY liq.id_liquidacion;
```

**Consulta de reinicio** (borra datos)

```sql
-- Consulta de reinicio. ATENCIÓN: BORRA datos. Solo para la base de desarrollo local (perfil dev).
-- Borra lo que generó la app (liquidaciones, partidas, snapshot, anulaciones y avisos) y devuelve los
-- resultados de sacrificio a NO_UTILIZADO. No borra el seed. Selecciona las seis sentencias y ejecútalas juntas.
DELETE FROM registro_anulacion;
DELETE FROM snapshot_datos_origen;
DELETE FROM partida_costo_lote;
DELETE FROM aviso_utilizacion_resultado;
DELETE FROM liquidacion;
UPDATE resultado_final_sacrificio SET estado_utilizacion = 'NO_UTILIZADO';

-- Opcional: para que los ids de liquidación vuelvan a empezar en 1.
-- ALTER SEQUENCE liquidacion_id_liquidacion_seq RESTART WITH 1;
```

--------------------------------------------------------------------------------
##### Edge Cases & Boundary Conditions
* **EC-001** : Si falla el guardado del registro de anulación, la liquidación no queda anulada (la anulación es atómica). Lo cubre la prueba automática `siFallaElRegistroNoQuedaAnulada`.
* **EC-002** : Dos usuarios anulan la misma liquidación al mismo tiempo: queda un solo registro. Lo cubre la prueba automática `dosAnulacionesSimultaneasDejanUnSoloRegistro`; a mano se acerca con la Sección 2.
* **EC-003** : Motivo de más de 500 caracteres. La pantalla limita la escritura a 500 caracteres. La API responde 400 `validacion`: "El motivo de anulación debe tener entre 10 y 500 caracteres." Verificado con la API.
* **EC-004** : Anular una liquidación que no existe (`PUT /api/v1/liquidaciones/999999/anulacion`). La API responde 404 `liquidacion-no-encontrada`. Verificado con la API.

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
