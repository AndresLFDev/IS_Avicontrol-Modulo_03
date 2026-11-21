### Test Case Specification: Consultar lista de lotes
**Created** : 2026-11-13  **Spec/Plan Link** : [spec.md](../../features/m3-cu01-lista-lotes/spec.md) · [002-consulta-lotes.md](../../planes/002-consulta-lotes.md)  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-M3-CU01-ListaLotes
**Product Name & Version** : AviControl Módulo 3 (rama `develop`, sin versión entregada)
**Author** : Juan Grau
**Current Owner** : Equipo Módulo 3
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that **la lista de lotes** (`GET /api/v1/lotes` y la pantalla "Lista de Lotes") functions successfully under specified operational conditions, y que el sistema:

- lista los lotes de la copia local sincronizada con su galpón y la etapa que M3 deriva (`Productivo`, `En Cosecha`, `Aislamiento`, `Por Liquidar`);
- habilita la acción "Generar liquidación" solo en los lotes `Por Liquidar` y la deja deshabilitada en los demás;
- filtra por etapa y busca por nombre de lote, nombre de galpón o UUID;
- no lista los lotes que ya tienen una liquidación `ACTIVA` (FR-014);
- muestra la fecha y hora de la última sincronización con Módulo 1 y con Módulo 2, y marca como desactualizada la fuente sin eventos exitosos en el umbral configurado.

Cubre los escenarios 1, 3 y 4 de la User Story 1 de la spec. El escenario 2 (lote en cosecha) se verifica en el paso 8.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| M3-CU01 Consultar lista de lotes | Specification Document | [features/m3-cu01-lista-lotes/spec.md](../../features/m3-cu01-lista-lotes/spec.md) |
| Plan 002 Consulta de lotes | Plan Document | [planes/002-consulta-lotes.md](../../planes/002-consulta-lotes.md) |
| Guía de pruebas manuales | Process Document | [proceso/guia-pruebas-manuales.md](../../proceso/guia-pruebas-manuales.md) |
| Consultas de apoyo | SQL | Al final de este caso: consultas 1, 2, 13b y 13c. También reunidas en [datos/consultas-demo.sql](../datos/consultas-demo.sql) |
| Reinicio de datos | SQL | Al final de este caso. El mismo archivo está en [datos/reset-demo.sql](../datos/reset-demo.sql) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La sincronización con Módulo 1 y Módulo 2 por REST no está implementada: la lista usa solo los datos del seed y la bitácora de sincronización no recibe filas nuevas.
* El paso 9 depende de la fecha del equipo: con el seed, ambas fuentes aparecen como desactualizadas porque su última sincronización es del 2026-10-20 y el umbral es de 24 horas (`sync.umbral-inactividad`).

##### Dependencies & Assumptions
* Entorno preparado según la [guía de pruebas manuales](../../proceso/guia-pruebas-manuales.md), sección 2.
* Base con el estado inicial del seed: sin liquidaciones. Si hay datos de una prueba anterior, ejecutar la consulta de reinicio (al final de este caso).

##### Preconditions & Default Setup
* **Initial State** : base de desarrollo con el seed `V1000` aplicado y sin liquidaciones.
* **Environment** : local. PostgreSQL en Docker, backend con perfil `dev`, frontend con `npm run dev:backend`.
* **Test Accounts** : no aplica, la aplicación no pide autenticación.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

**Última ejecución** : 2026-11-13. Los resultados del backend y de la base se verificaron contra la app real (`bootRun` con perfil `dev` y el seed), llamando a la API con los mismos datos y consultando con `psql`. Lo que muestra la pantalla se leyó en el código del frontend y no se vio en el navegador: esos pasos quedan en `[~]`.

##### Section 1: Consulta, filtro y búsqueda (escenarios 1, 3 y 4)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ver los datos de origen | Ejecutar las consultas 1 y 2 (al final de este caso) | N/A | Seis lotes con sus etapas: L-2026-01 `PRODUCTIVO`, 02 `EN_COSECHA`, 03 `POR_LIQUIDAR`, 04 `POR_LIQUIDAR`, 05 `AISLAMIENTO`, 06 `POR_LIQUIDAR`. Galpón Río sin lotes | [✔] | Verificado con la base |
| 2 | Abrir la lista | Abrir <http://localhost:5173/> | N/A | Título "Lista de Lotes". Aparecen 6 lotes, ordenados por fecha de ingreso: L-2026-03, 06, 04, 02, 05, 01. Galpón Río no aparece | [~] | API: 6 lotes en ese orden |
| 3 | Revisar las columnas | Mirar la tabla | N/A | Columnas Galpón, Lote, Etapa y Acciones, con la etapa en una etiqueta de color | [~] | Texto tomado del código |
| 4 | Revisar las acciones | Mirar el botón "Generar liquidación" de cada fila | N/A | Habilitado en L-2026-03, L-2026-04 y L-2026-06 (`POR_LIQUIDAR`). Deshabilitado en L-2026-01, 02 y 05 | [~] | API: `accionHabilitada` es `true` solo en esos tres |
| 5 | Filtrar por etapa | Elegir "Por Liquidar" en el filtro "Filtrar por etapa" | Etapa `POR_LIQUIDAR` | Quedan L-2026-03, 06 y 04. Con "Todos" vuelven los seis | [~] | API: `GET /api/v1/lotes?etapa=POR_LIQUIDAR` devuelve esos tres |
| 6 | Buscar por nombre de lote y de galpón | Escribir en "Buscar por nombre o ID…", un valor cada vez | `L-2026-03`, luego `Galpón Este` | Cada búsqueda deja solo L-2026-03 | [~] | API verificada con los dos valores |
| 7 | Buscar por UUID | Pegar el UUID del lote en el buscador | `b0000000-0000-0000-0000-000000000003` | Queda solo L-2026-03 | [~] | API verificada |
| 8 | Lote en cosecha | Elegir L-2026-02 (`EN_COSECHA`) | N/A | La acción de generar la liquidación está deshabilitada | [~] | Ver EC-001 |
| 9 | Revisar la sincronización | Mirar "Última sincronización" sobre la tabla | Seed: Módulo 1 13:07:01 UTC, Módulo 2 14:20:01 UTC del 2026-10-20 | Se muestran "Módulo 1: <fecha>" y "Módulo 2: <fecha>" (08:07 y 09:20 hora de Colombia), en rojo y con "(desactualizado)" | [~] | API: `GET /api/v1/sincronizacion/estado` devuelve esas fechas con `desactualizada: true` en las dos fuentes. Confirmar la hora que muestra la pantalla |
| 10 | Comparar con la base | Ejecutar las consultas 13b y 13c (al final de este caso) | N/A | La bitácora tiene 7 eventos `EXITOSA`. Las últimas exitosas coinciden con las del paso 9 | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Section 2: Un lote liquidado deja de aparecer (FR-014)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Generar una liquidación | Seguir la Sección 1 del caso [TC-M3-CU03](m3-cu03-generar-liquidacion.md) para L-2026-03 | Precio 4500 | La liquidación queda `ACTIVA` | [~] | Verificado con la API |
| 2 | Volver a la lista | Abrir <http://localhost:5173/> | N/A | L-2026-03 ya no aparece. La lista tiene 5 lotes | [~] | API: 5 lotes sin L-2026-03 |
| 3 | Anular y volver a mirar | Anular la liquidación (caso [TC-M3-CU04](m3-cu04-anular-liquidacion.md)) y abrir la lista | N/A | L-2026-03 vuelve a aparecer como `Por Liquidar` | [~] | Verificado con la API |
| 4 | Dejar la base limpia | Ejecutar la consulta de reinicio (al final de este caso) | N/A | La lista vuelve a tener 6 lotes | [✔] | Verificado con la base |

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

**Consulta 2**

```sql
-- 2. Galpones, incluido el que no tiene lote
-- Galpón Río está DISPONIBLE y no tiene lote, por eso la app no lo lista.
SELECT g.nombre, g.estado, g.aforo_maximo, COUNT(l.id_lote) AS lotes
  FROM galpon g
  LEFT JOIN lote l ON l.id_galpon = g.id_galpon
 GROUP BY g.id_galpon, g.nombre, g.estado, g.aforo_maximo
 ORDER BY g.nombre;
```

**Consulta 13b**

```sql
-- 13b. Bitácora de sincronización con M1 y M2. En el seed hay 7 eventos simulados, todos
--      EXITOSA. Una fila nueva aquí solo aparecería cuando se implemente la sincronización por
--      REST (CU07 a CU10); hoy la app no escribe en esta tabla.
SELECT id, fuente, topico, fecha_hora_inicio, fecha_hora_fin, resultado, registros_actualizados
  FROM registro_sincronizacion
 ORDER BY id;
```

**Consulta 13c**

```sql
-- 13c. Última sincronización exitosa por fuente: es lo que devuelve
--      GET /api/v1/sincronizacion/estado.
SELECT fuente, MAX(fecha_hora_fin) AS ultima_exitosa
  FROM registro_sincronizacion
 WHERE resultado = 'EXITOSA' AND fecha_hora_fin IS NOT NULL
 GROUP BY fuente
 ORDER BY fuente;
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
* **EC-001** : Un lote `En Cosecha` no se puede liquidar aunque se abra su URL de liquidación. Con L-2026-02 en el seed, el backend responde `422 lote-no-liquidable` con causa `SIN_ALERTA_VACIADO` (el mismo caso del paso 8 de [TC-M3-CU03](m3-cu03-generar-liquidacion.md) con L-2026-01).
* **EC-002** : La lista debe seguir funcionando aunque Módulo 1 o Módulo 2 no respondan, porque lee la copia local. Con el seed no se puede provocar: queda para cuando exista la sincronización.
* **EC-003** : Una búsqueda sin resultados (por ejemplo `zzzz`). La API responde 200 con la lista vacía y el mensaje "No se encontraron resultados". Pendiente de verificar cómo lo muestra la pantalla.
* **EC-004** : El frontend (<http://localhost:5173>) y el backend (<http://localhost:8080>) son orígenes distintos, así que el navegador exige CORS. Si la pantalla queda vacía y la consola del navegador muestra un error de CORS, revisar `cors.origenes-permitidos`. Lo cubren las pruebas automáticas `CorsConfigTest` (incluido el `PUT` de la anulación) y `SincronizacionControllerTest` (contrato del estado de sincronización del paso 9).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
