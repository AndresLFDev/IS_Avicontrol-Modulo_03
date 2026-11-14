-- consultas-demo.sql
-- Consultas de solo lectura para ver qué pasa en la base de datos de AviControl M3 mientras se
-- ejecutan los casos de prueba de docs/pruebas/casos/. No modifican nada.
--
-- Conexión (IntelliJ Database): host localhost, puerto 5433, base avicontrol,
-- usuario avicontrol, contraseña avicontrol (valores de desarrollo del compose.yaml y del .env).
--
-- Cómo ejecutar: abre una Query Console, pega este archivo, pon el cursor dentro de una consulta
-- y pulsa Ctrl+Enter.
--
-- Las secciones 1 a 5 muestran los datos de origen (el seed del perfil dev) y sirven antes de
-- generar nada. Las secciones 6 a 13 se miran después de usar la app. Para volver al estado
-- inicial usa reset-demo.sql (está en otro archivo a propósito: borra datos).
--
-- Los ids y los valores esperados salen del seed db/seed/V1000. Si ese seed cambia, hay que
-- revisar estas consultas y los casos de prueba.


-- =====================================================================================
-- 1. Lotes con su galpón y la etapa que calcula la app
-- Muestra los 6 lotes del seed, el estado del galpón y la etapa (la que ve la pantalla
-- "Lista de lotes"). La etapa sale de la alerta de vaciado y del estado del galpón, no del
-- estado del lote. Un lote con liquidación ACTIVA pasa a LIQUIDADO y la app lo oculta.
-- Esperado al inicio: L-2026-01 PRODUCTIVO, 02 EN_COSECHA, 03 POR_LIQUIDAR,
-- 04 POR_LIQUIDAR, 05 AISLAMIENTO, 06 POR_LIQUIDAR.
-- =====================================================================================
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


-- =====================================================================================
-- 2. Galpones, incluido el que no tiene lote
-- Galpón Río está DISPONIBLE y no tiene lote, por eso la app no lo lista.
-- =====================================================================================
SELECT g.nombre, g.estado, g.aforo_maximo, COUNT(l.id_lote) AS lotes
  FROM galpon g
  LEFT JOIN lote l ON l.id_galpon = g.id_galpon
 GROUP BY g.id_galpon, g.nombre, g.estado, g.aforo_maximo
 ORDER BY g.nombre;


-- =====================================================================================
-- 3. Resultado de sacrificio que envió M2
-- Dice cuántos pollos se vendieron y cuántos kilos pesaron. La venta bruta es
-- peso_total_kg por el precio por kg que se escribe en la app.
-- Esperado: L-2026-03 con 8.500 pollos y 23.800 kg; L-2026-06 con 8.600 y 24.510 kg;
-- L-2026-04 no tiene fila (siniestro total, murieron todos).
-- estado_utilizacion pasa a UTILIZADO cuando se genera la liquidación.
-- =====================================================================================
SELECT l.nombre AS lote, r.cantidad_final_pollos, r.peso_total_kg, r.estado_utilizacion
  FROM resultado_final_sacrificio r
  JOIN lote l ON l.id_lote = r.id_lote
 ORDER BY l.nombre;


-- =====================================================================================
-- 4. Partidas de alimento y de medicina de cada lote, con su subtotal
-- Cada partida de alimento cuesta cantidad_kg por precio_unitario_kg_cop. Cada tramo de
-- medicina cuesta cantidad por precio. Una partida con precio NULL o estado PENDIENTE_PRECIO
-- impide liquidar (error 422 PARTIDAS_SIN_PRECIO). Eso pasa con L-2026-06 (REC-2026-121).
-- =====================================================================================
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


-- =====================================================================================
-- 5. Costos operativos que debe dar cada lote
-- Costos operativos = costo de población + alimento + medicina. Esta consulta calcula el
-- número que la app debe mostrar. Compara después con la liquidación generada.
-- Esperado: L-2026-03 = 85.000.000 (18.000.000 + 64.800.000 + 2.200.000);
-- L-2026-04 = 32.800.000 (10.000.000 + 21.000.000 + 1.800.000).
-- L-2026-06 sale incompleto porque tiene una partida sin precio, que SUM ignora.
-- =====================================================================================
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


-- =====================================================================================
-- 6. Liquidaciones generadas
-- Una fila por liquidación. Mira estado (ACTIVA o ANULADA), el precio, la venta bruta, la
-- mortalidad, los costos y la utilidad. En siniestro total, precio_kg_cop y peso_promedio_kg
-- son NULL, pollos_vendidos es 0 y id_resultado_sacrificio es NULL.
-- Esperado para L-2026-03 con precio 4500: venta 107.100.000, costos 85.000.000,
-- utilidad 22.100.000, mortalidad 500 aves (5,56 %).
-- Esperado para L-2026-04: venta 0, costos 32.800.000, utilidad -32.800.000, mortalidad 100 %.
-- =====================================================================================
SELECT liq.id_liquidacion, l.nombre AS lote, liq.estado, liq.precio_kg_cop,
       liq.pollos_vendidos, liq.peso_total_kg, liq.venta_bruta_cop,
       liq.mortalidad_aves, liq.porcentaje_mortalidad,
       liq.costos_operativos_cop, liq.utilidad_neta_cop,
       liq.usuario_responsable, liq.fecha_hora_generacion
  FROM liquidacion liq
  JOIN lote l ON l.id_lote = liq.id_lote
 ORDER BY liq.id_liquidacion;


-- =====================================================================================
-- 7. Partidas congeladas de cada liquidación
-- Al generar, la app copia cada partida a partida_costo_lote. Así el desglose no cambia aunque
-- M1 o M2 envíen datos nuevos después. Debe haber partidas de ALIMENTO, MEDICINA y POBLACION.
-- Esperado para L-2026-03: Pre-inicio 15.600.000, Inicio 30.000.000, Engorde 19.200.000,
-- Amoxicilina 600.000 y 420.000 (dos recepciones), Vacuna Newcastle 1.180.000, población 18.000.000.
-- Con el seed el orden es siempre el mismo: las partidas se copian ordenadas por su id de origen.
-- =====================================================================================
SELECT p.id_liquidacion, p.categoria, p.concepto, p.cantidad, p.unidad_medida,
       p.precio_unitario_cop, p.subtotal_cop, p.fuente_origen, p.referencia_origen
  FROM partida_costo_lote p
 ORDER BY p.id_liquidacion, p.categoria, p.id_partida;


-- =====================================================================================
-- 8. Subtotales por categoría de cada liquidación
-- Debe coincidir con las tres tarjetas del desglose y sumar los costos operativos.
-- Esperado para L-2026-03: ALIMENTO 64.800.000, MEDICINA 2.200.000, POBLACION 18.000.000.
-- =====================================================================================
SELECT p.id_liquidacion, p.categoria, SUM(p.subtotal_cop) AS subtotal_cop
  FROM partida_costo_lote p
 GROUP BY p.id_liquidacion, p.categoria
 ORDER BY p.id_liquidacion, p.categoria;


-- =====================================================================================
-- 9. Datos de origen que se usaron (snapshot)
-- Una fila por fuente (MODULO_1 y MODULO_2) con la fecha de sincronización de los datos que
-- se usaron. La fila de MODULO_1 guarda además población inicial, población actual y costo del lote.
-- =====================================================================================
SELECT s.id_liquidacion, s.fuente, s.fecha_hora_sync,
       s.poblacion_inicial, s.poblacion_actual, s.costo_total_lote_cop
  FROM snapshot_datos_origen s
 ORDER BY s.id_liquidacion, s.fuente;


-- =====================================================================================
-- 10. Aviso de utilización para M2
-- Al generar una liquidación con resultado de sacrificio, el resultado pasa a UTILIZADO y se
-- crea un aviso PENDIENTE (M2 lo recibiría por Kafka; ese envío aún no está implementado).
-- Si se anula y se vuelve a liquidar, el aviso original se conserva y no se duplica.
-- En siniestro total no hay resultado, así que no se crea aviso.
-- =====================================================================================
SELECT r.id_resultado, l.nombre AS lote, r.estado_utilizacion,
       a.id_liquidacion AS liquidacion_del_aviso, a.estado_entrega, a.intentos
  FROM resultado_final_sacrificio r
  JOIN lote l ON l.id_lote = r.id_lote
  LEFT JOIN aviso_utilizacion_resultado a ON a.id_resultado = r.id_resultado
 ORDER BY l.nombre;


-- =====================================================================================
-- 11. Anulaciones registradas
-- Cada anulación guarda el motivo (10 a 500 caracteres), quién anuló y cuándo. La liquidación
-- anulada se conserva con todos sus valores; solo cambia su estado a ANULADA.
-- =====================================================================================
SELECT ra.id_anulacion, ra.id_liquidacion, l.nombre AS lote, ra.motivo,
       ra.usuario_responsable, ra.fecha_hora_anulacion
  FROM registro_anulacion ra
  JOIN liquidacion liq ON liq.id_liquidacion = ra.id_liquidacion
  JOIN lote l ON l.id_lote = liq.id_lote
 ORDER BY ra.id_anulacion;


-- =====================================================================================
-- 12. Comprobaciones de consistencia
-- 12a. Para cada liquidación: la suma de partidas debe igualar los costos operativos y
--      venta bruta menos costos debe igualar la utilidad. Las dos columnas deben dar true.
-- =====================================================================================
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

-- 12b. Nunca debe haber más de una liquidación ACTIVA por lote (lo garantiza el índice
--      uq_lote_activa). La columna activas debe ser 0 o 1 en todas las filas.
SELECT l.nombre AS lote,
       COUNT(*) FILTER (WHERE liq.estado = 'ACTIVA')  AS activas,
       COUNT(*) FILTER (WHERE liq.estado = 'ANULADA') AS anuladas
  FROM liquidacion liq
  JOIN lote l ON l.id_lote = liq.id_lote
 GROUP BY l.nombre
 ORDER BY l.nombre;

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


-- =====================================================================================
-- 13. Historial y sincronización
-- 13a. Lo mismo que muestra la pantalla "Historial de Liquidaciones": la más reciente primero,
--      con el galpón y lote, la venta, la mortalidad, los costos, la utilidad y el estado.
-- =====================================================================================
SELECT liq.fecha_hora_generacion, g.nombre AS galpon, l.nombre AS lote,
       liq.venta_bruta_cop, liq.porcentaje_mortalidad, liq.costos_operativos_cop,
       liq.utilidad_neta_cop, liq.estado
  FROM liquidacion liq
  JOIN lote l ON l.id_lote = liq.id_lote
  JOIN galpon g ON g.id_galpon = liq.id_galpon
 ORDER BY liq.fecha_hora_generacion DESC, liq.id_liquidacion DESC;

-- 13b. Bitácora de sincronización con M1 y M2. En el seed hay 7 eventos simulados, todos
--      EXITOSA. Una fila nueva aquí solo aparecería cuando se implementen los consumidores de
--      Kafka (CU07 a CU10); hoy la app no escribe en esta tabla.
SELECT id, fuente, topico, fecha_hora_inicio, fecha_hora_fin, resultado, registros_actualizados
  FROM registro_sincronizacion
 ORDER BY id;

-- 13c. Última sincronización exitosa por fuente: es lo que devuelve
--      GET /api/v1/sincronizacion/estado.
SELECT fuente, MAX(fecha_hora_fin) AS ultima_exitosa
  FROM registro_sincronizacion
 WHERE resultado = 'EXITOSA' AND fecha_hora_fin IS NOT NULL
 GROUP BY fuente
 ORDER BY fuente;
