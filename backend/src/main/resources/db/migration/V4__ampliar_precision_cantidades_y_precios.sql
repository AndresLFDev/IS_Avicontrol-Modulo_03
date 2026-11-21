-- V4__ampliar_precision_cantidades_y_precios.sql
-- Módulo 2 guarda cantidades y precios unitarios con 4 decimales (NUMERIC(18,4)). Con 3 y 2 decimales
-- M3 redondeaba antes de multiplicar y el costo de la Liquidación no coincidía con el de M2 (por
-- ejemplo, un medicamento a 0,0333 COP por unidad quedaba en 0,03). Solo ensancha columnas: no
-- pierde ni cambia ningún dato existente.

ALTER TABLE partida_alimento_lote
    ALTER COLUMN cantidad_kg             TYPE NUMERIC(18,4),
    ALTER COLUMN precio_unitario_kg_cop  TYPE NUMERIC(18,4);

ALTER TABLE consumo_medicamento_lote
    ALTER COLUMN cantidad_unidad_base    TYPE NUMERIC(18,4);

ALTER TABLE tramo_recepcion_consumo
    ALTER COLUMN cantidad_unidad_base    TYPE NUMERIC(18,4),
    ALTER COLUMN precio_unitario_base_cop TYPE NUMERIC(18,4);

-- La partida de costo conserva lo que se usó en la Liquidación (cantidad y precio sin redondear)
ALTER TABLE partida_costo_lote
    ALTER COLUMN cantidad                TYPE NUMERIC(18,4),
    ALTER COLUMN precio_unitario_cop     TYPE NUMERIC(18,4);
