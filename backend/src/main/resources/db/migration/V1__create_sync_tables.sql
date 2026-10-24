-- V1__create_sync_tables.sql — DDL de docs/arquitectura/general.md (sección B). No editar una vez aplicada.

CREATE TABLE registro_sincronizacion (
    id                     BIGSERIAL PRIMARY KEY,
    fuente                 VARCHAR(20)  NOT NULL,
    topico                 VARCHAR(100) NOT NULL,
    id_evento              UUID UNIQUE,
    fecha_hora_inicio      TIMESTAMPTZ  NOT NULL,
    fecha_hora_fin         TIMESTAMPTZ,
    resultado              VARCHAR(20),
    descripcion_error      TEXT,
    registros_actualizados INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_sync_fuente    CHECK (fuente IN ('MODULO_1', 'MODULO_2')),
    CONSTRAINT chk_sync_resultado CHECK (resultado IN ('EXITOSA', 'FALLIDA'))
);

CREATE TABLE galpon (
    id_galpon       UUID PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    aforo_maximo    INT          NOT NULL,
    estado          VARCHAR(30)  NOT NULL,
    fecha_hora_sync TIMESTAMPTZ  NOT NULL
);

CREATE TABLE lote (
    id_lote            UUID PRIMARY KEY,
    id_galpon          UUID         NOT NULL REFERENCES galpon(id_galpon),
    nombre             VARCHAR(100) NOT NULL,
    fecha_ingreso      DATE         NOT NULL,
    poblacion_inicial  INT          NOT NULL,
    poblacion_actual   INT          NOT NULL,
    costo_total_cop    BIGINT       NOT NULL,
    estado             VARCHAR(30)  NOT NULL,
    fecha_hora_sync    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE alerta_vaciado_sanitario (
    id_alerta                  UUID PRIMARY KEY,
    id_galpon                  UUID        NOT NULL REFERENCES galpon(id_galpon),
    id_lote                    UUID        NOT NULL UNIQUE REFERENCES lote(id_lote),
    fecha_hora_evento          TIMESTAMPTZ NOT NULL,
    id_registro_sincronizacion BIGINT      NOT NULL REFERENCES registro_sincronizacion(id)
);

CREATE TABLE resultado_final_sacrificio (
    id_resultado          UUID PRIMARY KEY,
    id_lote               UUID          NOT NULL UNIQUE REFERENCES lote(id_lote),
    id_galpon             UUID          NOT NULL REFERENCES galpon(id_galpon),
    cantidad_final_pollos INT           NOT NULL,
    peso_total_kg         NUMERIC(12,2) NOT NULL,
    fecha_registro        TIMESTAMPTZ   NOT NULL,
    fecha_hora_sync       TIMESTAMPTZ   NOT NULL,
    estado_utilizacion    VARCHAR(20)   NOT NULL DEFAULT 'NO_UTILIZADO',
    CONSTRAINT chk_resultado_utilizacion CHECK (estado_utilizacion IN ('NO_UTILIZADO', 'UTILIZADO'))
);

CREATE TABLE aviso_utilizacion_resultado (
    id_aviso           BIGSERIAL PRIMARY KEY,
    id_resultado       UUID        NOT NULL UNIQUE REFERENCES resultado_final_sacrificio(id_resultado),
    id_liquidacion     BIGINT      NOT NULL,
    fecha_hora_emision TIMESTAMPTZ NOT NULL,
    estado_entrega     VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos           INT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_aviso_estado CHECK (estado_entrega IN ('PENDIENTE', 'ENTREGADO'))
);
