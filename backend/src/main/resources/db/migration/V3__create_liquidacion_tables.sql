-- V3__create_liquidacion_tables.sql — DDL de docs/arquitectura/general.md (sección B). No editar una vez aplicada.

CREATE TABLE liquidacion (
    id_liquidacion          BIGSERIAL PRIMARY KEY,
    id_lote                 UUID          NOT NULL REFERENCES lote(id_lote),
    id_galpon               UUID          NOT NULL REFERENCES galpon(id_galpon),
    id_resultado_sacrificio UUID          REFERENCES resultado_final_sacrificio(id_resultado), -- NULL solo en siniestro total
    pollos_vendidos         INT           NOT NULL,                                            -- 0 en siniestro total
    peso_total_kg           NUMERIC(12,2) NOT NULL,                                            -- 0.00 en siniestro total
    peso_promedio_kg        NUMERIC(8,4),                                                      -- NULL en siniestro total
    precio_kg_cop           NUMERIC(14,2),                                                     -- NULL en siniestro total
    venta_bruta_cop         BIGINT        NOT NULL,
    mortalidad_aves         INT           NOT NULL,
    porcentaje_mortalidad   NUMERIC(5,2)  NOT NULL,
    costos_operativos_cop   BIGINT        NOT NULL,
    utilidad_neta_cop       BIGINT        NOT NULL,
    estado                  VARCHAR(10)   NOT NULL DEFAULT 'ACTIVA',
    fecha_hora_generacion   TIMESTAMPTZ   NOT NULL,
    usuario_responsable     VARCHAR(100)  NOT NULL,
    version                 BIGINT        NOT NULL DEFAULT 0,                                  -- bloqueo optimista (@Version)
    CONSTRAINT chk_liquidacion_estado CHECK (estado IN ('ACTIVA', 'ANULADA'))
);

-- Máximo una Liquidación ACTIVA por lote; las ANULADA se conservan y no bloquean una nueva
CREATE UNIQUE INDEX uq_lote_activa ON liquidacion(id_lote) WHERE estado = 'ACTIVA';

CREATE TABLE partida_costo_lote (
    id_partida          BIGSERIAL PRIMARY KEY,
    id_liquidacion      BIGINT        NOT NULL REFERENCES liquidacion(id_liquidacion),
    id_lote             UUID          NOT NULL REFERENCES lote(id_lote),
    categoria           VARCHAR(20)   NOT NULL,
    concepto            VARCHAR(200)  NOT NULL,
    cantidad            NUMERIC(12,3) NOT NULL,
    unidad_medida       VARCHAR(30)   NOT NULL,
    precio_unitario_cop NUMERIC(14,2) NOT NULL,
    subtotal_cop        BIGINT        NOT NULL,
    fuente_origen       VARCHAR(20)   NOT NULL,
    referencia_origen   VARCHAR(200),
    CONSTRAINT chk_partida_categoria CHECK (categoria IN ('ALIMENTO', 'MEDICINA', 'POBLACION')),
    CONSTRAINT chk_partida_fuente    CHECK (fuente_origen IN ('MODULO_1', 'MODULO_2'))
);

CREATE TABLE snapshot_datos_origen (
    id                   BIGSERIAL PRIMARY KEY,
    id_liquidacion       BIGINT      NOT NULL REFERENCES liquidacion(id_liquidacion),
    id_lote              UUID        NOT NULL REFERENCES lote(id_lote),
    fuente               VARCHAR(20) NOT NULL,
    fecha_hora_sync      TIMESTAMPTZ NOT NULL,
    poblacion_inicial    INT,
    poblacion_actual     INT,
    costo_total_lote_cop BIGINT,
    CONSTRAINT uq_snapshot_fuente UNIQUE (id_liquidacion, fuente)
);

CREATE TABLE registro_anulacion (
    id_anulacion         BIGSERIAL PRIMARY KEY,
    id_liquidacion       BIGINT       NOT NULL UNIQUE REFERENCES liquidacion(id_liquidacion),
    motivo               VARCHAR(500) NOT NULL,
    fecha_hora_anulacion TIMESTAMPTZ  NOT NULL,
    usuario_responsable  VARCHAR(100) NOT NULL
);

-- FK diferida desde aviso_utilizacion_resultado (tabla creada en V1)
ALTER TABLE aviso_utilizacion_resultado
ADD CONSTRAINT fk_aviso_liquidacion FOREIGN KEY (id_liquidacion) REFERENCES liquidacion(id_liquidacion);

-- Índices de rendimiento para consultas y joins por lote y galpón
CREATE INDEX idx_lote_galpon ON lote(id_galpon);
CREATE INDEX idx_alerta_vaciado_galpon ON alerta_vaciado_sanitario(id_galpon);
CREATE INDEX idx_partida_alimento_lote ON partida_alimento_lote(id_lote);
CREATE INDEX idx_consumo_med_lote ON consumo_medicamento_lote(id_lote);
CREATE INDEX idx_partida_costo_liquidacion ON partida_costo_lote(id_liquidacion);
CREATE INDEX idx_snapshot_liquidacion ON snapshot_datos_origen(id_liquidacion);
CREATE INDEX idx_liquidacion_lote ON liquidacion(id_lote);
CREATE INDEX idx_liquidacion_fecha ON liquidacion(fecha_hora_generacion DESC);
CREATE INDEX idx_sync_fuente_fin ON registro_sincronizacion(fuente, fecha_hora_fin DESC);
CREATE INDEX idx_aviso_pendiente ON aviso_utilizacion_resultado(estado_entrega) WHERE estado_entrega = 'PENDIENTE';
