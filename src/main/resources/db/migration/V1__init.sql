-- ─────────────────────────────────────────────────────────────
-- V1__init.sql  –  Schema com UUID como PK
-- ─────────────────────────────────────────────────────────────

-- ENUMs
CREATE TYPE perfil_enum           AS ENUM ('ANALISTA', 'GESTOR');
CREATE TYPE chamado_status_enum   AS ENUM ('AGUARDANDO', 'EM_ATENDIMENTO', 'AGUARDANDO_CLIENTE', 'ENCERRADO');
CREATE TYPE categoria_enum        AS ENUM ('ERRO', 'DUVIDA');
CREATE TYPE origem_enum           AS ENUM ('WHATSAPP', 'EMAIL', 'TELEFONE');
CREATE TYPE chat_origem_enum      AS ENUM ('CLIENTE', 'SUPORTE');
CREATE TYPE tipo_midia_enum       AS ENUM ('TEXTO', 'IMAGEM', 'AUDIO', 'VIDEO');

-- ─────────────────────────────────────────
-- CLIENTE
-- ─────────────────────────────────────────
CREATE TABLE cliente (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    nome                  VARCHAR(255) NOT NULL,
    cidade                VARCHAR(255),
    cpf_cnpj              VARCHAR(18)  NOT NULL,
    contato_principal     VARCHAR(255),
    comercial_responsavel VARCHAR(255),
    ativo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cliente_cpf_cnpj UNIQUE (cpf_cnpj)
);

-- ─────────────────────────────────────────
-- CONTATO
-- ─────────────────────────────────────────
CREATE TABLE contato (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    nome       VARCHAR(255) NOT NULL,
    telefone   VARCHAR(30)  NOT NULL,
    email      VARCHAR(255),
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_contato_telefone UNIQUE (telefone)
);

-- ─────────────────────────────────────────
-- CONTATO_CLIENTE (N:N)
-- ─────────────────────────────────────────
CREATE TABLE contato_cliente (
    id          UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    id_contato  UUID NOT NULL REFERENCES contato(id) ON DELETE RESTRICT,
    id_cliente  UUID NOT NULL REFERENCES cliente(id) ON DELETE RESTRICT,
    CONSTRAINT uq_contato_cliente UNIQUE (id_contato, id_cliente)
);

-- ─────────────────────────────────────────
-- USUARIO
-- ─────────────────────────────────────────
CREATE TABLE usuario (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    nome       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    senha      VARCHAR(255) NOT NULL,
    perfil     perfil_enum  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuario_email UNIQUE (email)
);

-- ─────────────────────────────────────────
-- TIPO / SUBTIPO
-- ─────────────────────────────────────────
CREATE TABLE tipo (
    id    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    nome  VARCHAR(255) NOT NULL,
    ativo BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE subtipo (
    id      UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    nome    VARCHAR(255) NOT NULL,
    ativo   BOOLEAN      NOT NULL DEFAULT TRUE,
    id_tipo UUID         NOT NULL REFERENCES tipo(id) ON DELETE RESTRICT
);

-- ─────────────────────────────────────────
-- CHAMADO
-- ─────────────────────────────────────────
CREATE TABLE chamado (
    id                       UUID                NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    texto                    TEXT,
    categoria                categoria_enum,
    solucao                  TEXT,
    status_atual             chamado_status_enum NOT NULL DEFAULT 'AGUARDANDO',
    dt_abertura              TIMESTAMP           NOT NULL,
    dt_encerramento          TIMESTAMP,
    dt_primeira_mensagem     TIMESTAMP,
    dt_primeira_resposta     TIMESTAMP,
    tempo_total_segundos     BIGINT,
    origem                   origem_enum         NOT NULL,
    id_contato               UUID                NOT NULL REFERENCES contato(id)  ON DELETE RESTRICT,
    id_usuario_responsavel   UUID                REFERENCES usuario(id)           ON DELETE SET NULL,
    id_subtipo               UUID                REFERENCES subtipo(id)           ON DELETE SET NULL,
    created_at               TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- CHAMADO_STATUS_HISTORICO
-- ─────────────────────────────────────────
CREATE TABLE chamado_status_historico (
    id                       UUID                NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    id_chamado               UUID                NOT NULL REFERENCES chamado(id) ON DELETE CASCADE,
    status                   chamado_status_enum NOT NULL,
    dt_inicio                TIMESTAMP           NOT NULL,
    dt_fim                   TIMESTAMP,
    tempo_em_status_segundos BIGINT,
    id_usuario_responsavel   UUID                REFERENCES usuario(id) ON DELETE SET NULL
);

-- ─────────────────────────────────────────
-- CHAT
-- ─────────────────────────────────────────
CREATE TABLE chat (
    id           UUID             NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    id_chamado   UUID             REFERENCES chamado(id) ON DELETE SET NULL,
    id_contato   UUID             NOT NULL REFERENCES contato(id) ON DELETE RESTRICT,
    id_usuario   UUID             REFERENCES usuario(id) ON DELETE SET NULL,
    origem       chat_origem_enum NOT NULL,
    dt_envio     TIMESTAMP        NOT NULL,
    texto        TEXT,
    file_url     VARCHAR(512),
    tipo_midia   tipo_midia_enum  NOT NULL DEFAULT 'TEXTO',
    fone_cliente VARCHAR(30)      NOT NULL,
    fone_suporte VARCHAR(30),
    nome_grupo   VARCHAR(255),
    nome_contato VARCHAR(255)     NOT NULL,
    created_at   TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────
-- ÍNDICES DE PERFORMANCE
-- ─────────────────────────────────────────
CREATE INDEX idx_chamado_contato        ON chamado(id_contato);
CREATE INDEX idx_chamado_usuario        ON chamado(id_usuario_responsavel);
CREATE INDEX idx_chamado_status         ON chamado(status_atual);
CREATE INDEX idx_chamado_subtipo        ON chamado(id_subtipo);

CREATE INDEX idx_historico_chamado      ON chamado_status_historico(id_chamado, dt_inicio);
CREATE INDEX idx_historico_dt_fim_null  ON chamado_status_historico(id_chamado)
    WHERE dt_fim IS NULL;

CREATE INDEX idx_chat_chamado_dt        ON chat(id_chamado, dt_envio);
CREATE INDEX idx_chat_sem_chamado       ON chat(fone_cliente) WHERE id_chamado IS NULL;
CREATE INDEX idx_chat_fone_cliente      ON chat(fone_cliente);

CREATE INDEX idx_contato_cliente_c      ON contato_cliente(id_contato);
CREATE INDEX idx_contato_cliente_cl     ON contato_cliente(id_cliente);