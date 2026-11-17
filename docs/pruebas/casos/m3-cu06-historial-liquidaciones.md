### Test Case Specification: Historial de liquidaciones
**Created** : 2026-11-13  **Spec/Plan Link** : [spec.md](../../features/m3-cu06-historial-liquidaciones/spec.md) · [004-reportes-historial.md](../../planes/004-reportes-historial.md)  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-M3-CU06-HistorialLiquidaciones
**Product Name & Version** : AviControl Módulo 3 (rama `develop`, sin versión entregada)
**Author** : Juan Grau
**Current Owner** : Equipo Módulo 3
**Priority** : P2 - High

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that **el historial de liquidaciones** (`GET /api/v1/liquidaciones` y la pantalla "Historial de Liquidaciones") functions successfully under specified operational conditions, y que el sistema:

- lista todas las liquidaciones, activas y anuladas, de la más reciente a la más antigua, con fecha y hora, galpón y lote, venta bruta, mortalidad, costos operativos, utilidad neta y estado;
- filtra por nombre de lote, nombre de galpón o UUID, y por rango de fechas;
- informa cuando ningún resultado cumple los criterios;
- rechaza un rango de fechas con la fecha inicial posterior a la final.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| M3-CU06 Historial de liquidaciones | Specification Document | [features/m3-cu06-historial-liquidaciones/spec.md](../../features/m3-cu06-historial-liquidaciones/spec.md) |
| Plan 004 Reportes e historial | Plan Document | [planes/004-reportes-historial.md](../../planes/004-reportes-historial.md) |
| Guía de pruebas manuales | Process Document | [proceso/guia-pruebas-manuales.md](../../proceso/guia-pruebas-manuales.md) |
| Consultas de apoyo | SQL | Al final de este caso: consultas 6 y 13a. También reunidas en [datos/consultas-demo.sql](../datos/consultas-demo.sql) |
| Reinicio de datos | SQL | Al final de este caso. El mismo archivo está en [datos/reset-demo.sql](../datos/reset-demo.sql) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La paginación (10 filas por página, máximo 50) no se puede probar con el seed: haría falta más de 10 liquidaciones. Queda pendiente para cuando haya más datos.
* Las fechas del filtro se comparan por día. El paso 8 usa la fecha del día en que se ejecuta la prueba.

##### Dependencies & Assumptions
* Entorno preparado según la [guía de pruebas manuales](../../proceso/guia-pruebas-manuales.md), sección 2.
* Los casos [TC-M3-CU03](m3-cu03-generar-liquidacion.md) y [TC-M3-CU04](m3-cu04-anular-liquidacion.md) pasaron: se pueden generar y anular liquidaciones.

##### Preconditions & Default Setup
* **Initial State** : base con el seed `V1000` y tres liquidaciones, que se crean en el paso 1: L-2026-03 con precio 4500 (anulada), L-2026-03 con precio 4600 (activa) y L-2026-04 (siniestro total, activa). Si hay otros datos, ejecutar la consulta de reinicio (al final de este caso) primero.
* **Environment** : local. PostgreSQL en Docker, backend con perfil `dev`, frontend con `npm run dev:backend`.
* **Test Accounts** : no aplica.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

**Última ejecución** : 2026-11-13. Los resultados del backend y de la base se verificaron contra la app real (`bootRun` con perfil `dev` y el seed), llamando a la API con los mismos datos y consultando con `psql`. Lo que muestra la pantalla se leyó en el código del frontend y no se vio en el navegador: esos pasos quedan en `[~]`.

##### Section 1: Consulta, búsqueda y filtro por fechas
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar tres liquidaciones | Generar y anular con los casos TC-M3-CU03 y TC-M3-CU04: L-2026-03 con precio 4500, anularla, generar L-2026-03 con precio 4600 y generar L-2026-04 | Ver las Preconditions | Existen una liquidación anulada y dos activas | [~] | Verificado con la API |
| 2 | Abrir el historial | Abrir <http://localhost:5173/historial> | N/A | Título "Historial de Liquidaciones" y una tabla con las columnas Fecha y hora, Galpón / Lote, Venta bruta, Mortalidad, Costos operativos, Utilidad neta y Estado | [~] | Columnas tomadas del código |
| 3 | Revisar el orden y los datos | Mirar las filas | N/A | De la más reciente a la más antigua: L-2026-04 activa (venta $0, costos $32.800.000, utilidad -$32.800.000, mortalidad 100 %), L-2026-03 activa (venta $109.480.000, costos $85.000.000, utilidad $24.480.000, mortalidad 5,56 %) y L-2026-03 anulada (venta $107.100.000, utilidad $22.100.000) | [~] | API: ese orden y esos valores. Los datos de cada fila: fecha, galpón, lote, venta, mortalidad, costos, utilidad y estado |
| 4 | Comparar con la base | Ejecutar la consulta 13a (al final de este caso) | N/A | El mismo orden y los mismos valores que la pantalla | [✔] | Verificado con la base (misma consulta que usa la app) |
| 5 | Buscar por nombre de lote | Escribir en "Buscar por nombre o UUID…" y esperar un momento | `L-2026-04` | Queda una sola fila, la de L-2026-04 | [~] | API verificada. La pantalla envía la búsqueda cuando se deja de escribir |
| 6 | Buscar por galpón y por UUID | Buscar por nombre de galpón y luego por UUID de lote | `Galpón Este`, luego `b0000000-0000-0000-0000-000000000003` | Quedan las dos liquidaciones de L-2026-03, la activa y la anulada | [~] | API verificada con los dos valores |
| 7 | Rango de fechas sin resultados | Elegir las fechas "desde" y "hasta" | Desde un día posterior a hoy, o hasta un día anterior al primer registro | No hay filas y aparece el mensaje de que ningún resultado cumple los criterios | [~] | API: lista vacía con el mensaje "No se encontraron liquidaciones para los criterios de búsqueda aplicados". Según el código, la pantalla muestra ese mensaje centrado en lugar de la tabla |
| 8 | Rango de fechas que incluye las liquidaciones | Elegir desde y hasta iguales a la fecha de hoy | Desde = hasta = fecha de hoy | Aparecen las tres liquidaciones | [~] | API verificada con la fecha del 2026-11-13 |
| 9 | Rango invertido | Elegir una fecha "desde" posterior a "hasta" | Desde `2026-12-01`, hasta `2026-01-01` | La pantalla no envía la consulta: avisa "La fecha inicial debe ser menor o igual a la fecha final." y marca los dos campos de fecha. La API, llamada directo, responde 400 `rango-fechas-invalido` | [~] | API verificada: 400. El aviso y el bloqueo salen del código |
| 10 | Abrir una liquidación desde el historial | Pulsar el enlace de acciones de una fila ("Ver liquidación del lote L-2026-03, ANULADA", para la anulada) | N/A | Se abre `/liquidaciones/<id>` con esa liquidación | [~] | Enlace tomado del código; el texto del enlace es el de su `aria-label` |
| 11 | Dejar la base limpia | Ejecutar la consulta de reinicio (al final de este caso) | N/A | Sin liquidaciones; el historial queda vacío | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Consultas SQL de este caso
Las consultas de apoyo y el reinicio están aquí mismo, listos para copiar; no hace falta abrir ningún otro archivo. La numeración es la de [`datos/consultas-demo.sql`](../datos/consultas-demo.sql), que las reúne todas. Conexión de desarrollo: host `localhost`, puerto `DB_PORT` del `.env` (5433), base `avicontrol`, usuario y contraseña `avicontrol`. En una consola SQL, pon el cursor dentro de la consulta y pulsa Ctrl+Enter. Las consultas son de solo lectura; solo la de reinicio borra datos.

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

**Consulta 13a**

```sql
-- 13a. Lo mismo que muestra la pantalla "Historial de Liquidaciones": la más reciente primero,
--      con el galpón y lote, la venta, la mortalidad, los costos, la utilidad y el estado.
SELECT liq.fecha_hora_generacion, g.nombre AS galpon, l.nombre AS lote,
       liq.venta_bruta_cop, liq.porcentaje_mortalidad, liq.costos_operativos_cop,
       liq.utilidad_neta_cop, liq.estado
  FROM liquidacion liq
  JOIN lote l ON l.id_lote = liq.id_lote
  JOIN galpon g ON g.id_galpon = liq.id_galpon
 ORDER BY liq.fecha_hora_generacion DESC, liq.id_liquidacion DESC;
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
* **EC-001** : Historial sin ninguna liquidación (base recién reiniciada). La API responde 200 con la lista vacía y el mensaje "No hay liquidaciones registradas aún"; según el código, la pantalla lo muestra en lugar de la tabla. Verificado con la API.
* **EC-002** : Búsqueda de más de 100 caracteres. La API responde 400. Verificado con la API; cómo lo muestra la pantalla queda por ver.
* **EC-003** : Paginación con más de 10 liquidaciones (ver Test Case Limitations).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
