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
| Consultas de apoyo | SQL Script | [datos/consultas-demo.sql](../datos/consultas-demo.sql) (secciones 6, 10, 11, 12b y 12c) |
| Reinicio de datos | SQL Script | [datos/reset-demo.sql](../datos/reset-demo.sql) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* No hay roles ni inicio de sesión: cualquier persona que abra la liquidación puede anularla. El usuario responsable es texto libre.
* La anulación de dos usuarios al mismo tiempo no se puede provocar a mano de forma confiable (ver EC-002).

##### Dependencies & Assumptions
* Entorno preparado según la [guía de pruebas manuales](../../proceso/guia-pruebas-manuales.md), sección 2.
* La Sección 1 del caso [TC-M3-CU03](m3-cu03-generar-liquidacion.md) pasó: existe una liquidación `ACTIVA` de L-2026-03. Este caso la usa como punto de partida.

##### Preconditions & Default Setup
* **Initial State** : base con el seed `V1000` y una liquidación `ACTIVA` de L-2026-03 con precio 4500 (venta 107.100.000, utilidad 22.100.000). Si hay otros datos, ejecutar [`reset-demo.sql`](../datos/reset-demo.sql) y generarla de nuevo.
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
| 6 | Revisar la liquidación | Ejecutar la sección 6 de `consultas-demo.sql` | N/A | El estado pasó a `ANULADA` y los valores no cambiaron | [✔] | Verificado con la base |
| 7 | Revisar el registro de anulación | Ejecutar la sección 11 | N/A | Una fila con el motivo `Precio por kg mal digitado`, el usuario `Juan Grau` y la fecha | [✔] | Verificado con la base |
| 8 | Comprobar la consistencia | Ejecutar la sección 12c | N/A | `coherente` en `t`: la anulada tiene un registro de anulación | [✔] | Verificado con la base |
| 9 | Ver la lista de lotes | Abrir <http://localhost:5173/> | N/A | L-2026-03 vuelve a aparecer como `Por Liquidar` | [~] | API: la lista vuelve a tener 6 lotes con L-2026-03 |

--------------------------------------------------------------------------------

##### Section 2: Anular una liquidación ya anulada
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Preparar una liquidación activa | Generar una liquidación nueva (Sección 3, pasos 1 a 3) | Precio `4600` | Liquidación `ACTIVA` | [~] | Verificado con la API |
| 2 | Abrirla en dos pestañas | Abrir la misma liquidación en dos pestañas del navegador | N/A | En las dos aparece el botón "Anular liquidación" | [ ] | Pendiente de ver en pantalla |
| 3 | Anular en la primera pestaña | Seguir la Sección 1, pasos 3 a 5 | Motivo `Precio por kg mal digitado` | Queda `ANULADA` | [~] | Verificado con la API |
| 4 | Anular en la segunda pestaña | En la segunda pestaña, que aún muestra el botón, intentar anular | Motivo `Precio por kg mal digitado` | 409 `liquidacion-ya-anulada`: "La Liquidación <id> ya fue anulada." con el registro de la anulación existente. La pantalla termina el flujo y muestra ese mensaje | [~] | API verificada: 409 con el `registroAnulacion` original. La pantalla trata este error de forma propia (`onTerminar`); el texto exacto queda por ver |
| 5 | Confirmar que sigue una sola anulación | Ejecutar la sección 11 | N/A | Una sola anulación por liquidación (la columna `id_liquidacion` es única) | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Section 3: Generar una liquidación nueva después de anular
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Entrar al flujo de generación | Desde la liquidación anulada pulsar "Generar nueva liquidación", o abrir L-2026-03 desde la lista | N/A | Se abre la ventana "Generar liquidación" del lote | [~] | Texto tomado del código |
| 2 | Pedir la vista previa con otro precio | Completar los campos y pulsar "Siguiente" | Precio `4600`, usuario `Juan Grau` | Venta bruta $109.480.000 (23.800 kg por 4.600), utilidad neta $24.480.000. Los costos siguen en $85.000.000 | [~] | API: venta 109.480.000, costos 85.000.000, utilidad 24.480.000 |
| 3 | Generar | Pulsar "Generar liquidación" | N/A | Nueva liquidación `ACTIVA` | [~] | API: 201 |
| 4 | Revisar las liquidaciones del lote | Ejecutar las secciones 6 y 12b | N/A | Dos filas de L-2026-03: una `ANULADA` con precio 4.500 y una `ACTIVA` con 4.600. `activas` igual a 1 y `anuladas` igual a 1 | [✔] | Verificado con la base |
| 5 | Revisar el aviso de utilización | Ejecutar la sección 10 | N/A | Una sola fila para L-2026-03, con `liquidacion_del_aviso` apuntando a la primera liquidación. No se duplica | [✔] | Verificado con la base. También lo cubre `trasAnularSePuedeReliquidarYElAvisoNoSeDuplica` |
| 6 | Dejar la base limpia | Ejecutar `reset-demo.sql` | N/A | Sin liquidaciones; la sección 1 vuelve a mostrar los tres lotes `POR_LIQUIDAR` | [✔] | Verificado con la base |

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
