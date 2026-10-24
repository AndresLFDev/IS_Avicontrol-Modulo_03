-- V2__create_partidas_tables.sql — DDL de docs/arquitectura/general.md (sección B). No editar una vez aplicada.

CREATE TABLE partida_alimento_lote (
    id_partida_origen      UUID PRIMARY KEY,
    id_lote                UUID          NOT NULL REFERENCES lote(id_lote),
    id_galpon              UUID          NOT NULL REFERENCES galpon(id_galpon),
    tipo_alimento          VARCHAR(50)   NOT NULL,
    cantidad_kg            NUMERIC(12,3) NOT NULL,
    precio_unitario_kg_cop NUMERIC(14,2),
    valor_impuesto_cop     NUMERIC(14,2),
    estado_valorizacion    VARCHAR(20)   NOT NULL DEFAULT 'VALORIZADA',
    referencia_origen      VARCHAR(200),
    fecha_hora_sync        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT chk_alimento_valorizacion CHECK (estado_valorizacion IN ('VALORIZADA', 'PENDIENTE_PRECIO'))
);

CREATE TABLE consumo_medicamento_lote (
    id_consumo_origen    UUID PRIMARY KEY,
    id_lote              UUID          NOT NULL REFERENCES lote(id_lote),
    id_galpon            UUID          NOT NULL REFERENCES galpon(id_galpon),
    medicamento          VARCHAR(150)  NOT NULL,
    fecha_consumo        DATE          NOT NULL,
    cantidad_unidad_base NUMERIC(12,3) NOT NULL,
    unidad_base          VARCHAR(30)   NOT NULL,
    fecha_hora_sync      TIMESTAMPTZ   NOT NULL
);

CREATE TABLE tramo_recepcion_consumo (
    id_tramo                 BIGSERIAL PRIMARY KEY,
    id_consumo_origen        UUID          NOT NULL REFERENCES consumo_medicamento_lote(id_consumo_origen),
    referencia_recepcion     VARCHAR(200)  NOT NULL,
    cantidad_unidad_base     NUMERIC(12,3) NOT NULL,
    precio_unitario_base_cop NUMERIC(14,2),
    valor_impuesto_cop       NUMERIC(14,2),
    estado_valorizacion      VARCHAR(20)   NOT NULL DEFAULT 'VALORIZADA',
    CONSTRAINT uq_tramo_recepcion UNIQUE (id_consumo_origen, referencia_recepcion),
    CONSTRAINT chk_tramo_valorizacion CHECK (estado_valorizacion IN ('VALORIZADA', 'PENDIENTE_PRECIO'))
);
