-- reset-demo.sql
-- ATENCIÓN: este archivo BORRA datos. Solo para la base de desarrollo local (perfil dev).
--
-- Borra lo que generó la app al usar los casos de prueba: liquidaciones, partidas, snapshot,
-- anulaciones y avisos, y devuelve los resultados de sacrificio a NO_UTILIZADO. NO borra el seed
-- (galpones, lotes, partidas de alimento, medicinas, alertas, resultados ni la bitácora de
-- sincronización).
--
-- El orden respeta las claves foráneas. Selecciona las seis sentencias y ejecútalas juntas
-- (Ctrl+Enter). Después ejecuta la sección 1 de consultas-demo.sql: los tres lotes POR_LIQUIDAR
-- deben volver a aparecer.

DELETE FROM registro_anulacion;
DELETE FROM snapshot_datos_origen;
DELETE FROM partida_costo_lote;
DELETE FROM aviso_utilizacion_resultado;
DELETE FROM liquidacion;
UPDATE resultado_final_sacrificio SET estado_utilizacion = 'NO_UTILIZADO';

-- Los ids de liquidación siguen aumentando después del borrado (1, 2, 3 ...). Si quieres que
-- vuelvan a empezar en 1, ejecuta además:
-- ALTER SEQUENCE liquidacion_id_liquidacion_seq RESTART WITH 1;
