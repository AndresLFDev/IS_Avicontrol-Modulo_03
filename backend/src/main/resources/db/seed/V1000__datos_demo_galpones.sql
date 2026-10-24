-- Datos de demostración (solo perfil dev). Simulan la copia local que llenarían los eventos de M1 y M2.
--
-- | Galpón          | Estado            | Lote       | Etapa derivada | Para probar                                           |
-- |-----------------|-------------------|------------|----------------|-------------------------------------------------------|
-- | Galpón Norte    | PRODUCTIVO        | L-2026-01  | PRODUCTIVO     | Acción deshabilitada                                  |
-- | Galpón Sur      | EN_COSECHA        | L-2026-02  | EN_COSECHA     | Acción deshabilitada                                  |
-- | Galpón Este     | VACIADO_SANITARIO | L-2026-03  | POR_LIQUIDAR   | Caso Dorado de CU03 esc. 1 (costos $85.000.000)       |
-- | Galpón Oeste    | VACIADO_SANITARIO | L-2026-04  | POR_LIQUIDAR   | Siniestro total (población 0, costos $32.800.000)     |
-- | Galpón Central  | AISLAMIENTO       | L-2026-05  | AISLAMIENTO    | Acción deshabilitada                                  |
-- | Galpón Alto     | VACIADO_SANITARIO | L-2026-06  | POR_LIQUIDAR   | 422 PARTIDAS_SIN_PRECIO                               |
-- | Galpón Río      | DISPONIBLE        | —          | (no se lista)  | Galpón sin lote                                       |

-- Bitácora de los "eventos" que habrían importado estos datos
INSERT INTO registro_sincronizacion (fuente, topico, id_evento, fecha_hora_inicio, fecha_hora_fin, resultado, registros_actualizados) VALUES
  ('MODULO_1', 'avicontrol.m1.galpones',               'e0000000-0000-0000-0000-000000000001', '2026-10-20 08:00:00-05', '2026-10-20 08:00:01-05', 'EXITOSA', 13),
  ('MODULO_1', 'avicontrol.m1.alertas-vaciado',        'e0000000-0000-0000-0000-000000000003', '2026-10-20 08:05:00-05', '2026-10-20 08:05:01-05', 'EXITOSA', 1),
  ('MODULO_1', 'avicontrol.m1.alertas-vaciado',        'e0000000-0000-0000-0000-000000000004', '2026-10-20 08:06:00-05', '2026-10-20 08:06:01-05', 'EXITOSA', 1),
  ('MODULO_1', 'avicontrol.m1.alertas-vaciado',        'e0000000-0000-0000-0000-000000000006', '2026-10-20 08:07:00-05', '2026-10-20 08:07:01-05', 'EXITOSA', 1),
  ('MODULO_2', 'avicontrol.m2.resultados-sacrificio',  'e0000000-0000-0000-0000-000000000010', '2026-10-20 09:00:00-05', '2026-10-20 09:00:01-05', 'EXITOSA', 2),
  ('MODULO_2', 'avicontrol.m2.partidas-alimento',      'e0000000-0000-0000-0000-000000000011', '2026-10-20 09:10:00-05', '2026-10-20 09:10:01-05', 'EXITOSA', 6),
  ('MODULO_2', 'avicontrol.m2.consumos-medicamento',   'e0000000-0000-0000-0000-000000000012', '2026-10-20 09:20:00-05', '2026-10-20 09:20:01-05', 'EXITOSA', 3);

INSERT INTO galpon (id_galpon, nombre, aforo_maximo, estado, fecha_hora_sync) VALUES
  ('a0000000-0000-0000-0000-000000000001', 'Galpón Norte',   10000, 'PRODUCTIVO',        '2026-10-20 08:00:01-05'),
  ('a0000000-0000-0000-0000-000000000002', 'Galpón Sur',     10000, 'EN_COSECHA',        '2026-10-20 08:00:01-05'),
  ('a0000000-0000-0000-0000-000000000003', 'Galpón Este',    10000, 'VACIADO_SANITARIO', '2026-10-20 08:00:01-05'),
  ('a0000000-0000-0000-0000-000000000004', 'Galpón Oeste',    9000, 'VACIADO_SANITARIO', '2026-10-20 08:00:01-05'),
  ('a0000000-0000-0000-0000-000000000005', 'Galpón Central',  8000, 'AISLAMIENTO',       '2026-10-20 08:00:01-05'),
  ('a0000000-0000-0000-0000-000000000006', 'Galpón Alto',     9000, 'VACIADO_SANITARIO', '2026-10-20 08:00:01-05'),
  ('a0000000-0000-0000-0000-000000000007', 'Galpón Río',      7000, 'DISPONIBLE',        '2026-10-20 08:00:01-05');

INSERT INTO lote (id_lote, id_galpon, nombre, fecha_ingreso, poblacion_inicial, poblacion_actual, costo_total_cop, estado, fecha_hora_sync) VALUES
  ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'L-2026-01', '2026-09-20', 9500, 9420, 19000000, 'ACTIVO',  '2026-10-20 08:00:01-05'),
  ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'L-2026-02', '2026-08-25', 9800, 9550, 19600000, 'ACTIVO',  '2026-10-20 08:00:01-05'),
  ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'L-2026-03', '2026-08-01', 9000, 8500, 18000000, 'CERRADO', '2026-10-20 08:00:01-05'),
  ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'L-2026-04', '2026-08-05', 8000,    0, 10000000, 'CERRADO', '2026-10-20 08:00:01-05'),
  ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005', 'L-2026-05', '2026-09-10', 7800, 7610, 15600000, 'ACTIVO',  '2026-10-20 08:00:01-05'),
  ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006', 'L-2026-06', '2026-08-03', 8800, 8650, 17600000, 'CERRADO', '2026-10-20 08:00:01-05');

INSERT INTO alerta_vaciado_sanitario (id_alerta, id_galpon, id_lote, fecha_hora_evento, id_registro_sincronizacion) VALUES
  ('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', '2026-10-18 07:15:00-05',
   (SELECT id FROM registro_sincronizacion WHERE id_evento = 'e0000000-0000-0000-0000-000000000003')),
  ('c0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', '2026-10-18 10:30:00-05',
   (SELECT id FROM registro_sincronizacion WHERE id_evento = 'e0000000-0000-0000-0000-000000000004')),
  ('c0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', '2026-10-19 06:45:00-05',
   (SELECT id FROM registro_sincronizacion WHERE id_evento = 'e0000000-0000-0000-0000-000000000006'));

-- L-2026-03: 8.500 pollos y 23.800 kg (CU03 esc. 1). L-2026-04 no tiene resultado (siniestro total).
INSERT INTO resultado_final_sacrificio (id_resultado, id_lote, id_galpon, cantidad_final_pollos, peso_total_kg, fecha_registro, fecha_hora_sync) VALUES
  ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 8500, 23800.00, '2026-10-17 18:00:00-05', '2026-10-20 09:00:01-05'),
  ('d0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006', 8600, 24510.00, '2026-10-18 17:30:00-05', '2026-10-20 09:00:01-05');

-- Alimento. L-2026-03: 15.600.000 + 30.000.000 + 19.200.000 = 64.800.000
--           L-2026-04: 21.000.000
--           L-2026-06: una partida sin precio (PENDIENTE_PRECIO)
INSERT INTO partida_alimento_lote (id_partida_origen, id_lote, id_galpon, tipo_alimento, cantidad_kg, precio_unitario_kg_cop, valor_impuesto_cop, estado_valorizacion, referencia_origen, fecha_hora_sync) VALUES
  ('f0000000-0000-0000-0000-000000000031', 'b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Pre-inicio',  6000.000, 2600.00,  780000.00, 'VALORIZADA',       'REC-2026-101', '2026-10-20 09:10:01-05'),
  ('f0000000-0000-0000-0000-000000000032', 'b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Inicio',     12000.000, 2500.00, 1500000.00, 'VALORIZADA',       'REC-2026-108', '2026-10-20 09:10:01-05'),
  ('f0000000-0000-0000-0000-000000000033', 'b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Engorde',     8000.000, 2400.00,  960000.00, 'VALORIZADA',       'REC-2026-118', '2026-10-20 09:10:01-05'),
  ('f0000000-0000-0000-0000-000000000041', 'b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'Inicio',      7000.000, 3000.00, 1050000.00, 'VALORIZADA',       'REC-2026-109', '2026-10-20 09:10:01-05'),
  ('f0000000-0000-0000-0000-000000000061', 'b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006', 'Inicio',     11000.000, 2500.00, 1375000.00, 'VALORIZADA',       'REC-2026-110', '2026-10-20 09:10:01-05'),
  ('f0000000-0000-0000-0000-000000000062', 'b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006', 'Engorde',     9000.000,    NULL,       NULL, 'PENDIENTE_PRECIO', 'REC-2026-121', '2026-10-20 09:10:01-05');

-- Medicamentos. L-2026-03: 600.000 + 420.000 (dos recepciones) + 1.180.000 = 2.200.000
--               L-2026-04: 1.800.000
INSERT INTO consumo_medicamento_lote (id_consumo_origen, id_lote, id_galpon, medicamento, fecha_consumo, cantidad_unidad_base, unidad_base, fecha_hora_sync) VALUES
  ('90000000-0000-0000-0000-000000000031', 'b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Amoxicilina',          '2026-08-20',   50.000, 'ML',     '2026-10-20 09:20:01-05'),
  ('90000000-0000-0000-0000-000000000032', 'b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'Vacuna Newcastle',     '2026-08-08', 9440.000, 'UNIDAD', '2026-10-20 09:20:01-05'),
  ('90000000-0000-0000-0000-000000000041', 'b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'Enrofloxacina',        '2026-08-22',   90.000, 'ML',     '2026-10-20 09:20:01-05');

INSERT INTO tramo_recepcion_consumo (id_consumo_origen, referencia_recepcion, cantidad_unidad_base, precio_unitario_base_cop, valor_impuesto_cop, estado_valorizacion) VALUES
  ('90000000-0000-0000-0000-000000000031', 'REC-2026-090',   30.000, 20000.00, 30000.00, 'VALORIZADA'),
  ('90000000-0000-0000-0000-000000000031', 'REC-2026-101',   20.000, 21000.00, 21000.00, 'VALORIZADA'),
  ('90000000-0000-0000-0000-000000000032', 'REC-2026-085', 9440.000,   125.00, 59000.00, 'VALORIZADA'),
  ('90000000-0000-0000-0000-000000000041', 'REC-2026-095',   90.000, 20000.00, 90000.00, 'VALORIZADA');
