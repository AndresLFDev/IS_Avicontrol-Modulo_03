### Test Case Specification: Generar liquidación
**Created** : 2026-11-13  **Spec/Plan Link** : [spec.md](../../features/m3-cu03-generar-liquidacion/spec.md) · [003-gestion-liquidaciones.md](../../planes/003-gestion-liquidaciones.md)  **Status** : Draft

#### Test Overview  *(mandatory)*

**Test Case ID** : TC-M3-CU03-GenerarLiquidacion
**Product Name & Version** : AviControl Módulo 3 (rama `develop`, sin versión entregada)
**Author** : Juan Grau
**Current Owner** : Equipo Módulo 3
**Priority** : P1 - Critical

--------------------------------------------------------------------------------

#### Objectives & Context  *(mandatory)*

##### Test Objectives
This test case verifies that **la generación de la liquidación de un lote** (`POST /api/v1/liquidaciones/previa` y `POST /api/v1/liquidaciones`, desde la ventana "Generar liquidación") functions successfully under specified operational conditions, y que el sistema:

- calcula la vista previa sin guardar nada, con la venta bruta, los costos operativos, la utilidad neta y la mortalidad;
- al confirmar, guarda la liquidación `ACTIVA` con sus partidas de costo congeladas, los datos de origen usados y el aviso de utilización del resultado de sacrificio;
- liquida un siniestro total sin pedir precio por kg;
- rechaza con un mensaje claro el precio o el usuario inválidos, el lote sin alerta de vaciado, el lote inexistente y el lote con una partida sin precio;
- permite una sola liquidación `ACTIVA` por lote;
- avisa y no guarda cuando los datos de origen cambian entre la vista previa y la confirmación.

##### Document References
| Reference Name | Type of Reference | Document Location / Link |
| ------ | ------ | ------ |
| M3-CU03 Generar liquidación | Specification Document | [features/m3-cu03-generar-liquidacion/spec.md](../../features/m3-cu03-generar-liquidacion/spec.md) |
| Plan 003 Gestión de liquidaciones | Plan Document | [planes/003-gestion-liquidaciones.md](../../planes/003-gestion-liquidaciones.md) |
| Guía de pruebas manuales | Process Document | [proceso/guia-pruebas-manuales.md](../../proceso/guia-pruebas-manuales.md) |
| Consultas de apoyo | SQL Script | [datos/consultas-demo.sql](../datos/consultas-demo.sql) (secciones 3 a 12) |
| Reinicio de datos | SQL Script | [datos/reset-demo.sql](../datos/reset-demo.sql) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* El aviso de utilización se guarda como `PENDIENTE` en la base, pero no se publica a Módulo 2: el envío por Kafka no está implementado.
* La pantalla no deja probar por sí sola algunos casos: el lote sin alerta de vaciado tiene el botón deshabilitado y el lote inexistente no tiene botón. Esos pasos se hacen con Swagger (<http://localhost:8080/swagger-ui.html>) o con una llamada a la API.
* El paso 4 de la Sección 4 usa dos pestañas para simular a dos usuarios que actúan casi al mismo tiempo.

##### Dependencies & Assumptions
* Entorno preparado según la [guía de pruebas manuales](../../proceso/guia-pruebas-manuales.md), sección 2.
* El caso [TC-M3-CU01](m3-cu01-lista-lotes.md) pasó: la lista muestra los lotes del seed.
* Cada sección parte del estado inicial: antes de la Sección 1, de la 2 y de la 4, ejecutar [`reset-demo.sql`](../datos/reset-demo.sql).

##### Preconditions & Default Setup
* **Initial State** : base con el seed `V1000` y sin liquidaciones.
* **Environment** : local. PostgreSQL en Docker, backend con perfil `dev`, frontend con `npm run dev:backend`.
* **Test Accounts** : no aplica. El usuario responsable es texto libre de 3 a 100 caracteres; en los pasos se usa `Juan Grau`.

--------------------------------------------------------------------------------

#### Test Execution Flow  *(mandatory)*

**Última ejecución** : 2026-11-13. Los resultados del backend y de la base se verificaron contra la app real (`bootRun` con perfil `dev` y el seed), llamando a la API con los mismos datos y consultando con `psql`. Lo que muestra la pantalla se leyó en el código del frontend y no se vio en el navegador: esos pasos quedan en `[~]`.

##### Section 1: Caso Dorado, vista previa y generación
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Ver lo que debe dar el lote | Ejecutar las secciones 3 y 5 de `consultas-demo.sql` | N/A | L-2026-03: 8.500 pollos, 23.800 kg, costos operativos 85.000.000 (18.000.000 + 64.800.000 + 2.200.000) | [✔] | Verificado con la base |
| 2 | Abrir la ventana | En la lista, pulsar "Generar liquidación" en L-2026-03 | N/A | Ventana "Generar liquidación" con "Galpón Este · Lote L-2026-03", el campo "Precio por kg (COP)" y el campo "Usuario responsable" | [~] | Texto tomado del código |
| 3 | Pedir la vista previa | Completar los campos y pulsar "Siguiente" | Precio `4500`, usuario `Juan Grau` | Aparece la vista previa con el aviso "Vista previa · sin generar" y la Matriz de Venta Final: venta bruta $107.100.000, costos operativos $85.000.000, utilidad neta $22.100.000, mortalidad 500 aves (5,56 %) | [~] | API: venta 107.100.000, costos 85.000.000, utilidad 22.100.000, mortalidad 500 y 5,56. Peso promedio 2,8 kg |
| 4 | Comprobar que no se guardó | Ejecutar la sección 6 de `consultas-demo.sql` | N/A | Sin filas: la vista previa no guarda nada | [✔] | Verificado con la base |
| 5 | Generar | Pulsar "Generar liquidación" en la vista previa | N/A | Se abre la liquidación (`/liquidaciones/<id>`) en estado Activa, con los botones "Ver desglose" y "Anular liquidación" | [~] | API: responde 201 con estado `ACTIVA` y los mismos valores |
| 6 | Revisar la liquidación guardada | Ejecutar las secciones 6 a 9 de `consultas-demo.sql` | N/A | Sección 6: una fila `ACTIVA` con venta 107.100.000, costos 85.000.000, utilidad 22.100.000, mortalidad 500 y 5,56 %. Sección 7: partidas de `ALIMENTO` (3), `MEDICINA` (3) y `POBLACION` (1). Sección 8: `ALIMENTO` 64.800.000, `MEDICINA` 2.200.000, `POBLACION` 18.000.000. Sección 9: una fila `MODULO_1` y otra `MODULO_2` | [✔] | Verificado con la base |
| 7 | Revisar el aviso de utilización | Ejecutar la sección 10 de `consultas-demo.sql` | N/A | El resultado de L-2026-03 queda `UTILIZADO` y hay un aviso `PENDIENTE` con 0 intentos | [✔] | Verificado con la base |
| 8 | Comprobar la consistencia | Ejecutar las secciones 12a y 12b | N/A | `costos_cuadran` y `utilidad_cuadra` en `t`. `activas` igual a 1 para L-2026-03 | [✔] | Verificado con la base |
| 9 | Volver a la lista | Pulsar "← Volver a la lista de lotes" | N/A | L-2026-03 ya no aparece | [~] | API: la lista queda con 5 lotes |

--------------------------------------------------------------------------------

##### Section 2: Siniestro total (L-2026-04)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Abrir la ventana | Pulsar "Generar liquidación" en L-2026-04 | N/A | Aparece "Siniestro total: el lote no tuvo venta, así que no se ingresa precio por kg" y no hay campo de precio | [~] | Texto tomado del código |
| 2 | Pedir la vista previa | Escribir el usuario y pulsar "Siguiente" | Usuario `Juan Grau` | Venta bruta $0, costos operativos $32.800.000, utilidad neta -$32.800.000, mortalidad 100 % | [~] | API: venta 0, costos 32.800.000, utilidad -32.800.000, mortalidad 8.000 aves y 100 % |
| 3 | Generar | Pulsar "Generar liquidación" | N/A | Liquidación `ACTIVA` con esos valores | [~] | API: 201 |
| 4 | Revisar en la base | Ejecutar las secciones 6 y 10 | N/A | Sección 6: la fila de L-2026-04 tiene `precio_kg_cop` vacío, `pollos_vendidos` 0, venta 0, costos 32.800.000, utilidad -32.800.000. Sección 10: no hay aviso para L-2026-04 (no tiene resultado de sacrificio) | [✔] | Verificado con la base |
| 5 | Enviar un precio a un siniestro total | En Swagger, `POST /api/v1/liquidaciones/previa` con el lote y un precio | Lote `b0000000-0000-0000-0000-000000000004`, precio `4500` | 400 `validacion` en el campo `precioKgCop`: "El lote es un siniestro total: no se ingresa precio por kg." | [✔] | Verificado con la API |

--------------------------------------------------------------------------------

##### Section 3: Validaciones y lotes no liquidables
Partir de la base sin liquidaciones.

| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Precio igual a cero o negativo | En L-2026-03, pulsar "Siguiente" con precio inválido | Precio `0`, luego `-5`, usuario `Juan Grau` | 400 `validacion`. La pantalla muestra el aviso "La solicitud contiene parámetros inválidos." y, bajo el campo de precio, "El precio por kg debe ser mayor que 0" | [~] | API verificada con los dos valores. El formulario no bloquea nada antes de enviar |
| 2 | Precio vacío | Pulsar "Siguiente" sin precio | Precio vacío | 400 `validacion`: "El precio por kg es obligatorio." bajo el campo de precio | [~] | API verificada |
| 3 | Usuario vacío o corto | Pulsar "Siguiente" con el usuario inválido | Usuario vacío, luego `Ju` | 400 `validacion` en el campo `usuario`. Con el usuario de 2 caracteres: "El usuario responsable debe tener entre 3 y 100 caracteres." | [~] | API verificada. Con el usuario vacío la API devuelve dos mensajes del campo (longitud y obligatorio) y la pantalla muestra el primero |
| 4 | Lote con una partida sin precio | En L-2026-06, escribir precio y usuario y pulsar "Siguiente" | Precio `4500`, usuario `Juan Grau` | 422 `lote-no-liquidable`, causa `PARTIDAS_SIN_PRECIO`. La pantalla muestra "Hay partidas de costo sin precio aplicable. No es posible liquidar." y la lista "Alimento: Engorde (REC-2026-121)" | [~] | API verificada. Texto de pantalla tomado del código |
| 5 | Revisar la partida en la base | Ejecutar la sección 4 de `consultas-demo.sql` | N/A | La fila `L-2026-06 / Engorde / REC-2026-121` tiene precio vacío y estado `PENDIENTE_PRECIO` | [✔] | Verificado con la base |
| 6 | Lote sin alerta de vaciado | En Swagger, `POST /api/v1/liquidaciones/previa` | Lote L-2026-01 `b0000000-0000-0000-0000-000000000001`, precio `4500`, usuario `Juan Grau` | 422 `lote-no-liquidable`, causa `SIN_ALERTA_VACIADO`. En la pantalla el botón de ese lote está deshabilitado | [✔] | API verificada. El botón deshabilitado está en [TC-M3-CU01](m3-cu01-lista-lotes.md) |
| 7 | Lote que no existe | En Swagger, el mismo `POST` con un UUID inventado | Lote `00000000-0000-0000-0000-00000000dead` | 404 `lote-no-encontrado` | [✔] | Verificado con la API |
| 8 | Confirmar que nada se guardó | Ejecutar la sección 6 de `consultas-demo.sql` | N/A | Sin filas nuevas después de los pasos 1 a 7 | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Section 4: Una sola liquidación activa y vista previa desactualizada
Partir de la base sin liquidaciones.

| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Generar la liquidación de L-2026-03 | Seguir la Sección 1, pasos 2 a 5 | Precio `4500` | Liquidación `ACTIVA` | [~] | Verificado con la API |
| 2 | Abrir la URL de un lote ya liquidado | En el navegador, abrir `/lotes/b0000000-0000-0000-0000-000000000003/liquidacion` | N/A | La ventana dice "Este lote no está disponible para liquidar: ya tiene una liquidación activa o no existe." y no muestra el formulario | [~] | Texto tomado del código. El lote no está en la lista, así que la ventana no lo encuentra |
| 3 | Segunda liquidación activa por la API | En Swagger, `POST /api/v1/liquidaciones/previa` y `POST /api/v1/liquidaciones` para el mismo lote | Lote L-2026-03, precio `4500` | Las dos llamadas responden 409 `liquidacion-ya-existente` con "El lote … ya tiene una Liquidación ACTIVA (ID: <id>). Anúlela antes de generar una nueva." y el campo `idLiquidacionActiva` | [✔] | Verificado con la API |
| 4 | Dos pestañas a la vez | Reiniciar los datos. Abrir la ventana de L-2026-03 en dos pestañas y pedir la vista previa en las dos. Generar en la primera. Pulsar "Generar liquidación" en la segunda | Precio `4500` | En la segunda pestaña aparece el aviso del paso 3 y el enlace "Ver liquidación" que lleva a la liquidación activa | [ ] | Pendiente de ver en pantalla. Por la API se prueba con el paso 3 |
| 5 | Confirmar que sigue una sola | Ejecutar las secciones 6 y 12b | N/A | Una sola fila para L-2026-03. `activas` igual a 1 | [✔] | Verificado con la base |
| 6 | Vista previa desactualizada | Reiniciar los datos. En L-2026-03 llegar a la vista previa y no confirmar. En la base ejecutar `UPDATE partida_alimento_lote SET precio_unitario_kg_cop = 2450.00 WHERE id_partida_origen = 'f0000000-0000-0000-0000-000000000033';`. Volver a la pantalla y pulsar "Generar liquidación" | Precio `4500` | 409 `vista-previa-desactualizada`: "Los datos sincronizados del lote cambiaron después de la vista previa. Revise la nueva vista previa antes de confirmar." La pantalla recalcula, muestra los valores nuevos y el aviso "Los datos sincronizados del lote cambiaron. Revise los valores actualizados antes de generar la liquidación." No se guarda nada | [~] | API verificada: responde 409 y la sección 6 queda sin filas. Los valores nuevos y el aviso salen del código |
| 7 | Devolver el dato | En la base ejecutar `UPDATE partida_alimento_lote SET precio_unitario_kg_cop = 2400.00 WHERE id_partida_origen = 'f0000000-0000-0000-0000-000000000033';` | N/A | La vista previa de L-2026-03 vuelve a dar costos de $85.000.000 | [✔] | Verificado con la API. Si no se devuelve el dato, el Caso Dorado deja de dar $85.000.000 |

--------------------------------------------------------------------------------

##### Edge Cases & Boundary Conditions
* **EC-001** : Dos usuarios generan la liquidación del mismo lote al mismo tiempo. La base lo impide con el índice único `uq_lote_activa`; el segundo recibe 409 `liquidacion-ya-existente`. Está cubierto por la prueba automática `dosConfirmacionesSimultaneasDejanUnaSolaActiva` de `LiquidacionIT`; a mano no se puede provocar de forma confiable.
* **EC-002** : El usuario cierra la ventana en la vista previa. No queda nada guardado (Sección 1, paso 4).
* **EC-003** : Un lote sin alerta de vaciado no se puede liquidar aunque su galpón esté productivo (Sección 3, paso 6).
* **EC-004** : Un lote con población actual 0 se liquida como siniestro total sin precio (Sección 2).

--------------------------------------------------------------------------------

#### Defect Severity Reference

| Severity Level | Code | Definition & Impact |
| ------ | :---: | ------ |
| **Critical** | **C** | System crash, data corruption, or complete block of critical user journey with no workaround. |
| **Major** | **M** | Core feature broken or significant functionality loss, but an awkward workaround exists. |
| **Small / Minor** | **S** | Cosmetic issue, minor UI alignment, typo, or non-blocking defect. |
