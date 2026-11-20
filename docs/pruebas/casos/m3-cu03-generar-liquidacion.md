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
| Consultas de apoyo | SQL | Al final de este caso: consultas 3, 4, 5, 6, 7, 8, 9, 10, 12a y 12b. También reunidas en [datos/consultas-demo.sql](../datos/consultas-demo.sql) |
| Reinicio de datos | SQL | Al final de este caso. El mismo archivo está en [datos/reset-demo.sql](../datos/reset-demo.sql) |

--------------------------------------------------------------------------------

#### Prerequisites & Environment

##### Test Case Limitations
* El aviso de utilización se guarda como `PENDIENTE` en la base, pero no se publica a Módulo 2: el envío no está implementado.
* La pantalla no deja probar por sí sola algunos casos: el lote sin alerta de vaciado tiene el botón deshabilitado y el lote inexistente no tiene botón. Esos pasos se hacen con Swagger (<http://localhost:8080/swagger-ui.html>) o con una llamada a la API.
* El paso 4 de la Sección 4 usa dos pestañas para simular a dos usuarios que actúan casi al mismo tiempo.

##### Dependencies & Assumptions
* Entorno preparado según la [guía de pruebas manuales](../../proceso/guia-pruebas-manuales.md), sección 2.
* El caso [TC-M3-CU01](m3-cu01-lista-lotes.md) pasó: la lista muestra los lotes del seed.
* Cada sección parte del estado inicial: antes de la Sección 1, de la 2 y de la 4, ejecutar la consulta de reinicio (al final de este caso).

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
| 1 | Ver lo que debe dar el lote | Ejecutar las consultas 3 y 5 (al final de este caso) | N/A | L-2026-03: 8.500 pollos, 23.800 kg, costos operativos 85.000.000 (18.000.000 + 64.800.000 + 2.200.000) | [✔] | Verificado con la base |
| 2 | Abrir la ventana | En la lista, pulsar "Generar liquidación" en L-2026-03 | N/A | Ventana "Generar liquidación" con "Galpón Este · Lote L-2026-03", el campo "Precio por kg (COP)" y el campo "Usuario responsable" | [~] | Texto tomado del código |
| 3 | Pedir la vista previa | Completar los campos y pulsar "Siguiente" | Precio `4500`, usuario `Juan Grau` | Aparece la vista previa con el aviso "Vista previa · sin generar" y la Matriz de Venta Final: venta bruta $107.100.000, costos operativos $85.000.000, utilidad neta $22.100.000, mortalidad 500 aves (5,56 %) | [~] | API: venta 107.100.000, costos 85.000.000, utilidad 22.100.000, mortalidad 500 y 5,56. Peso promedio 2,8 kg |
| 4 | Comprobar que no se guardó | Ejecutar la consulta 6 (al final de este caso) | N/A | Sin filas: la vista previa no guarda nada | [✔] | Verificado con la base |
| 5 | Generar | Pulsar "Generar liquidación" en la vista previa | N/A | Se abre la liquidación (`/liquidaciones/<id>`) en estado Activa, con los botones "Ver desglose" y "Anular liquidación" | [~] | API: responde 201 con estado `ACTIVA` y los mismos valores |
| 6 | Revisar la liquidación guardada | Ejecutar las consultas 6 a 9 (al final de este caso) | N/A | Consulta 6: una fila `ACTIVA` con venta 107.100.000, costos 85.000.000, utilidad 22.100.000, mortalidad 500 y 5,56 %. Consulta 7: partidas de `ALIMENTO` (3), `MEDICINA` (3) y `POBLACION` (1). Consulta 8: `ALIMENTO` 64.800.000, `MEDICINA` 2.200.000, `POBLACION` 18.000.000. Consulta 9: una fila `MODULO_1` y otra `MODULO_2` | [✔] | Verificado con la base |
| 7 | Revisar el aviso de utilización | Ejecutar la consulta 10 (al final de este caso) | N/A | El resultado de L-2026-03 queda `UTILIZADO` y hay un aviso `PENDIENTE` con 0 intentos | [✔] | Verificado con la base |
| 8 | Comprobar la consistencia | Ejecutar las consultas 12a y 12b (al final de este caso) | N/A | `costos_cuadran` y `utilidad_cuadra` en `t`. `activas` igual a 1 para L-2026-03 | [✔] | Verificado con la base |
| 9 | Volver a la lista | Pulsar "← Volver a la lista de lotes" | N/A | L-2026-03 ya no aparece | [~] | API: la lista queda con 5 lotes |

--------------------------------------------------------------------------------

##### Section 2: Siniestro total (L-2026-04)
| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Abrir la ventana | Pulsar "Generar liquidación" en L-2026-04 | N/A | Aparece "Siniestro total: el lote no tuvo venta, así que no se ingresa precio por kg" y no hay campo de precio | [~] | Texto tomado del código |
| 2 | Pedir la vista previa | Escribir el usuario y pulsar "Siguiente" | Usuario `Juan Grau` | Venta bruta $0, costos operativos $32.800.000, utilidad neta -$32.800.000, mortalidad 100 % | [~] | API: venta 0, costos 32.800.000, utilidad -32.800.000, mortalidad 8.000 aves y 100 % |
| 3 | Generar | Pulsar "Generar liquidación" | N/A | Liquidación `ACTIVA` con esos valores | [~] | API: 201 |
| 4 | Revisar en la base | Ejecutar las consultas 6 y 10 (al final de este caso) | N/A | Consulta 6: la fila de L-2026-04 tiene `precio_kg_cop` vacío, `pollos_vendidos` 0, venta 0, costos 32.800.000, utilidad -32.800.000. Consulta 10: no hay aviso para L-2026-04 (no tiene resultado de sacrificio) | [✔] | Verificado con la base |
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
| 5 | Revisar la partida en la base | Ejecutar la consulta 4 (al final de este caso) | N/A | La fila `L-2026-06 / Engorde / REC-2026-121` tiene precio vacío y estado `PENDIENTE_PRECIO` | [✔] | Verificado con la base |
| 6 | Lote sin alerta de vaciado | En Swagger, `POST /api/v1/liquidaciones/previa` | Lote L-2026-01 `b0000000-0000-0000-0000-000000000001`, precio `4500`, usuario `Juan Grau` | 422 `lote-no-liquidable`, causa `SIN_ALERTA_VACIADO`. En la pantalla el botón de ese lote está deshabilitado | [✔] | API verificada. El botón deshabilitado está en [TC-M3-CU01](m3-cu01-lista-lotes.md) |
| 7 | Lote que no existe | En Swagger, el mismo `POST` con un UUID inventado | Lote `00000000-0000-0000-0000-00000000dead` | 404 `lote-no-encontrado` | [✔] | Verificado con la API |
| 8 | Confirmar que nada se guardó | Ejecutar la consulta 6 (al final de este caso) | N/A | Sin filas nuevas después de los pasos 1 a 7 | [✔] | Verificado con la base |

--------------------------------------------------------------------------------

##### Section 4: Una sola liquidación activa y vista previa desactualizada
Partir de la base sin liquidaciones.

| Step # | Step Description | Path & Action | Test Data | Expected Results | Pass/Fail | Comments |
| :---: | ------ | ------ | ------ | ------ | :---: | ------ |
| 1 | Generar la liquidación de L-2026-03 | Seguir la Sección 1, pasos 2 a 5 | Precio `4500` | Liquidación `ACTIVA` | [~] | Verificado con la API |
| 2 | Abrir la URL de un lote ya liquidado | En el navegador, abrir `/lotes/b0000000-0000-0000-0000-000000000003/liquidacion` | N/A | La ventana dice "Este lote no está disponible para liquidar: ya tiene una liquidación activa o no existe." y no muestra el formulario | [~] | Texto tomado del código. El lote no está en la lista, así que la ventana no lo encuentra |
| 3 | Segunda liquidación activa por la API | En Swagger, `POST /api/v1/liquidaciones/previa` y `POST /api/v1/liquidaciones` para el mismo lote | Lote L-2026-03, precio `4500` | Las dos llamadas responden 409 `liquidacion-ya-existente` con "El lote … ya tiene una Liquidación ACTIVA (ID: <id>). Anúlela antes de generar una nueva." y el campo `idLiquidacionActiva` | [✔] | Verificado con la API |
| 4 | Dos pestañas a la vez | Reiniciar los datos. Abrir la ventana de L-2026-03 en dos pestañas y pedir la vista previa en las dos. Generar en la primera. Pulsar "Generar liquidación" en la segunda | Precio `4500` | En la segunda pestaña aparece el aviso del paso 3 y el enlace "Ver liquidación" que lleva a la liquidación activa | [ ] | Pendiente de ver en pantalla. Por la API se prueba con el paso 3 |
| 5 | Confirmar que sigue una sola | Ejecutar las consultas 6 y 12b (al final de este caso) | N/A | Una sola fila para L-2026-03. `activas` igual a 1 | [✔] | Verificado con la base |
| 6 | Vista previa desactualizada | Reiniciar los datos. En L-2026-03 llegar a la vista previa y no confirmar. En la base ejecutar `UPDATE partida_alimento_lote SET precio_unitario_kg_cop = 2450.00 WHERE id_partida_origen = 'f0000000-0000-0000-0000-000000000033';`. Volver a la pantalla y pulsar "Generar liquidación" | Precio `4500` | 409 `vista-previa-desactualizada`: "Los datos sincronizados del lote cambiaron después de la vista previa. Revise la nueva vista previa antes de confirmar." La pantalla recalcula, muestra los valores nuevos y el aviso "Los datos sincronizados del lote cambiaron. Revise los valores actualizados antes de generar la liquidación." No se guarda nada | [~] | API verificada: responde 409 y la sección 6 queda sin filas. Los valores nuevos y el aviso salen del código |
| 7 | Devolver el dato | En la base ejecutar `UPDATE partida_alimento_lote SET precio_unitario_kg_cop = 2400.00 WHERE id_partida_origen = 'f0000000-0000-0000-0000-000000000033';` | N/A | La vista previa de L-2026-03 vuelve a dar costos de $85.000.000 | [✔] | Verificado con la API. Si no se devuelve el dato, el Caso Dorado deja de dar $85.000.000 |

--------------------------------------------------------------------------------

##### Consultas SQL de este caso
Las consultas de apoyo y el reinicio están aquí mismo, listos para copiar; no hace falta abrir ningún otro archivo. La numeración es la de [`datos/consultas-demo.sql`](../datos/consultas-demo.sql), que las reúne todas. Conexión de desarrollo: host `localhost`, puerto `DB_PORT` del `.env` (5433), base `avicontrol`, usuario y contraseña `avicontrol`. En una consola SQL, pon el cursor dentro de la consulta y pulsa Ctrl+Enter. Las consultas son de solo lectura; solo la de reinicio borra datos.

**Consulta 3**

```sql
-- 3. Resultado de sacrificio que envió M2
-- Dice cuántos pollos se vendieron y cuántos kilos pesaron. La venta bruta es
-- peso_total_kg por el precio por kg que se escribe en la app.
-- Esperado: L-2026-03 con 8.500 pollos y 23.800 kg; L-2026-06 con 8.600 y 24.510 kg;
-- L-2026-04 no tiene fila (siniestro total, murieron todos).
-- estado_utilizacion pasa a UTILIZADO cuando se genera la liquidación.
SELECT l.nombre AS lote, r.cantidad_final_pollos, r.peso_total_kg, r.estado_utilizacion
  FROM resultado_final_sacrificio r
  JOIN lote l ON l.id_lote = r.id_lote
 ORDER BY l.nombre;
```

**Consulta 4**

```sql
-- 4. Partidas de alimento y de medicina de cada lote, con su subtotal
-- Cada partida de alimento cuesta cantidad_kg por precio_unitario_kg_cop. Cada tramo de
-- medicina cuesta cantidad por precio. Una partida con precio NULL o estado PENDIENTE_PRECIO
-- impide liquidar (error 422 PARTIDAS_SIN_PRECIO). Eso pasa con L-2026-06 (REC-2026-121).
SELECT l.nombre AS lote, 'ALIMENTO' AS categoria, p.tipo_alimento AS concepto,
       p.referencia_origen AS referencia, p.cantidad_kg AS cantidad,
       p.precio_unitario_kg_cop AS precio, p.estado_valorizacion,
       ROUND(p.cantidad_kg * p.precio_unitario_kg_cop) AS subtotal_cop
  FROM partida_alimento_lote p
  JOIN lote l ON l.id_lote = p.id_lote
UNION ALL
SELECT l.nombre, 'MEDICINA', c.medicamento,
       t.referencia_recepcion, t.cantidad_unidad_base,
       t.precio_unitario_base_cop, t.estado_valorizacion,
       ROUND(t.cantidad_unidad_base * t.precio_unitario_base_cop)
  FROM consumo_medicamento_lote c
  JOIN tramo_recepcion_consumo t ON t.id_consumo_origen = c.id_consumo_origen
  JOIN lote l ON l.id_lote = c.id_lote
 ORDER BY 1, 2, 3;
```

**Consulta 5**

```sql
-- 5. Costos operativos que debe dar cada lote
-- Costos operativos = costo de población + alimento + medicina. Esta consulta calcula el
-- número que la app debe mostrar. Compara después con la liquidación generada.
-- Esperado: L-2026-03 = 85.000.000 (18.000.000 + 64.800.000 + 2.200.000);
-- L-2026-04 = 32.800.000 (10.000.000 + 21.000.000 + 1.800.000).
-- L-2026-06 sale incompleto porque tiene una partida sin precio, que SUM ignora.
SELECT l.nombre AS lote,
       l.costo_total_cop                                        AS poblacion,
       COALESCE(al.total, 0)                                    AS alimento,
       COALESCE(me.total, 0)                                    AS medicina,
       l.costo_total_cop + COALESCE(al.total, 0) + COALESCE(me.total, 0) AS costos_operativos
  FROM lote l
  LEFT JOIN (SELECT id_lote, ROUND(SUM(cantidad_kg * precio_unitario_kg_cop)) AS total
               FROM partida_alimento_lote GROUP BY id_lote) al ON al.id_lote = l.id_lote
  LEFT JOIN (SELECT c.id_lote, ROUND(SUM(t.cantidad_unidad_base * t.precio_unitario_base_cop)) AS total
               FROM consumo_medicamento_lote c
               JOIN tramo_recepcion_consumo t ON t.id_consumo_origen = c.id_consumo_origen
              GROUP BY c.id_lote) me ON me.id_lote = l.id_lote
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

**Consulta 7**

```sql
-- 7. Partidas congeladas de cada liquidación
-- Al generar, la app copia cada partida a partida_costo_lote. Así el desglose no cambia aunque
-- M1 o M2 envíen datos nuevos después. Debe haber partidas de ALIMENTO, MEDICINA y POBLACION.
-- Esperado para L-2026-03: Pre-inicio 15.600.000, Inicio 30.000.000, Engorde 19.200.000,
-- Amoxicilina 600.000 y 420.000 (dos recepciones), Vacuna Newcastle 1.180.000, población 18.000.000.
-- Con el seed el orden es siempre el mismo: las partidas se copian ordenadas por su id de origen.
SELECT p.id_liquidacion, p.categoria, p.concepto, p.cantidad, p.unidad_medida,
       p.precio_unitario_cop, p.subtotal_cop, p.fuente_origen, p.referencia_origen
  FROM partida_costo_lote p
 ORDER BY p.id_liquidacion, p.categoria, p.id_partida;
```

**Consulta 8**

```sql
-- 8. Subtotales por categoría de cada liquidación
-- Debe coincidir con las tres tarjetas del desglose y sumar los costos operativos.
-- Esperado para L-2026-03: ALIMENTO 64.800.000, MEDICINA 2.200.000, POBLACION 18.000.000.
SELECT p.id_liquidacion, p.categoria, SUM(p.subtotal_cop) AS subtotal_cop
  FROM partida_costo_lote p
 GROUP BY p.id_liquidacion, p.categoria
 ORDER BY p.id_liquidacion, p.categoria;
```

**Consulta 9**

```sql
-- 9. Datos de origen que se usaron (snapshot)
-- Una fila por fuente (MODULO_1 y MODULO_2) con la fecha de sincronización de los datos que
-- se usaron. La fila de MODULO_1 guarda además población inicial, población actual y costo del lote.
SELECT s.id_liquidacion, s.fuente, s.fecha_hora_sync,
       s.poblacion_inicial, s.poblacion_actual, s.costo_total_lote_cop
  FROM snapshot_datos_origen s
 ORDER BY s.id_liquidacion, s.fuente;
```

**Consulta 10**

```sql
-- 10. Aviso de utilización para M2
-- Al generar una liquidación con resultado de sacrificio, el resultado pasa a UTILIZADO y se
-- crea un aviso PENDIENTE (M2 lo recibiría por el canal que se acuerde; ese envío aún no está implementado).
-- Si se anula y se vuelve a liquidar, el aviso original se conserva y no se duplica.
-- En siniestro total no hay resultado, así que no se crea aviso.
SELECT r.id_resultado, l.nombre AS lote, r.estado_utilizacion,
       a.id_liquidacion AS liquidacion_del_aviso, a.estado_entrega, a.intentos
  FROM resultado_final_sacrificio r
  JOIN lote l ON l.id_lote = r.id_lote
  LEFT JOIN aviso_utilizacion_resultado a ON a.id_resultado = r.id_resultado
 ORDER BY l.nombre;
```

**Consulta 12a**

```sql
-- 12a. Para cada liquidación: la suma de partidas debe igualar los costos operativos y
--      venta bruta menos costos debe igualar la utilidad. Las dos columnas deben dar true.
SELECT liq.id_liquidacion, l.nombre AS lote,
       liq.costos_operativos_cop,
       COALESCE(SUM(p.subtotal_cop), 0)                                       AS suma_partidas,
       liq.costos_operativos_cop = COALESCE(SUM(p.subtotal_cop), 0)           AS costos_cuadran,
       liq.venta_bruta_cop - liq.costos_operativos_cop = liq.utilidad_neta_cop AS utilidad_cuadra
  FROM liquidacion liq
  JOIN lote l ON l.id_lote = liq.id_lote
  LEFT JOIN partida_costo_lote p ON p.id_liquidacion = liq.id_liquidacion
 GROUP BY liq.id_liquidacion, l.nombre
 ORDER BY liq.id_liquidacion;
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
