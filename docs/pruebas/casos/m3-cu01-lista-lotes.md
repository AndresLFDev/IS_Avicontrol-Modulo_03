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
| Consultas de apoyo | SQL Script | [datos/consultas-demo.sql](../datos/consultas-demo.sql) (secciones 1, 2, 13b y 13c) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* La sincronización con Módulo 1 y Módulo 2 por Kafka no está implementada: la lista usa solo los datos del seed y la bitácora de sincronización no recibe filas nuevas.
* El paso 9 depende de la fecha del equipo: con el seed, ambas fuentes aparecen como desactualizadas porque su última sincronización es del 2026-10-20 y el umbral es de 24 horas (`sync.umbral-inactividad`).

##### Dependencies & Assumptions
* Entorno preparado según la [guía de pruebas manuales](../../proceso/guia-pruebas-manuales.md), sección 2.
* Base con el estado inicial del seed: sin liquidaciones. Si hay datos de una prueba anterior, ejecutar [`reset-demo.sql`](../datos/reset-demo.sql).

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
| 1 | Ver los datos de origen | Ejecutar las secciones 1 y 2 de `consultas-demo.sql` | N/A | Seis lotes con sus etapas: L-2026-01 `PRODUCTIVO`, 02 `EN_COSECHA`, 03 `POR_LIQUIDAR`, 04 `POR_LIQUIDAR`, 05 `AISLAMIENTO`, 06 `POR_LIQUIDAR`. Galpón Río sin lotes | [✔] | Verificado con la base |
| 2 | Abrir la lista | Abrir <http://localhost:5173/> | N/A | Título "Lista de Lotes". Aparecen 6 lotes, ordenados por fecha de ingreso: L-2026-03, 06, 04, 02, 05, 01. Galpón Río no aparece | [~] | API: 6 lotes en ese orden |
| 3 | Revisar las columnas | Mirar la tabla | N/A | Columnas Galpón, Lote, Etapa y Acciones, con la etapa en una etiqueta de color | [~] | Texto tomado del código |
| 4 | Revisar las acciones | Mirar el botón "Generar liquidación" de cada fila | N/A | Habilitado en L-2026-03, L-2026-04 y L-2026-06 (`POR_LIQUIDAR`). Deshabilitado en L-2026-01, 02 y 05 | [~] | API: `accionHabilitada` es `true` solo en esos tres |
| 5 | Filtrar por etapa | Elegir "Por Liquidar" en el filtro "Filtrar por etapa" | Etapa `POR_LIQUIDAR` | Quedan L-2026-03, 06 y 04. Con "Todos" vuelven los seis | [~] | API: `GET /api/v1/lotes?etapa=POR_LIQUIDAR` devuelve esos tres |
| 6 | Buscar por nombre de lote y de galpón | Escribir en "Buscar por nombre o ID…", un valor cada vez | `L-2026-03`, luego `Galpón Este` | Cada búsqueda deja solo L-2026-03 | [~] | API verificada con los dos valores |
| 7 | Buscar por UUID | Pegar el UUID del lote en el buscador | `b0000000-0000-0000-0000-000000000003` | Queda solo L-2026-03 | [~] | API verificada |
| 8 | Lote en cosecha | Elegir L-2026-02 (`EN_COSECHA`) | N/A | La acción de generar la liquidación está deshabilitada | [~] | Ver EC-001 |
| 9 | Revisar la sincronización | Mirar "Última sincronización" sobre la tabla | Seed: Módulo 1 13:07:01 UTC, Módulo 2 14:20:01 UTC del 2026-10-20 | Se muestran "Módulo 1: <fecha>" y "Módulo 2: <fecha>" (08:07 y 09:20 hora de Colombia), en rojo y con "(desactualizado)" | [~] | API: `GET /api/v1/sincronizacion/estado` devuelve esas fechas con `desactualizada: true` en las dos fuentes. Confirmar la hora que muestra la pantalla |
| 10 | Comparar con la base | Ejecutar las secciones 13b y 13c de `consultas-demo.sql` | N/A | La bitácora tiene 7 eventos `EXITOSA`. Las últimas exitosas coinciden con las del paso 9 | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Section 2: Un lote liquidado deja de aparecer (FR-014)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Generar una liquidación | Seguir la Sección 1 del caso [TC-M3-CU03](m3-cu03-generar-liquidacion.md) para L-2026-03 | Precio 4500 | La liquidación queda `ACTIVA` | [~] | Verificado con la API |
| 2 | Volver a la lista | Abrir <http://localhost:5173/> | N/A | L-2026-03 ya no aparece. La lista tiene 5 lotes | [~] | API: 5 lotes sin L-2026-03 |
| 3 | Anular y volver a mirar | Anular la liquidación (caso [TC-M3-CU04](m3-cu04-anular-liquidacion.md)) y abrir la lista | N/A | L-2026-03 vuelve a aparecer como `Por Liquidar` | [~] | Verificado con la API |
| 4 | Dejar la base limpia | Ejecutar `reset-demo.sql` | N/A | La lista vuelve a tener 6 lotes | [✔] | Verificado con la base |

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
