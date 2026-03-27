-- ─────────────────────────────────────────────────────────────
-- V4__convert_enums_to_varchar.sql
-- ─────────────────────────────────────────────────────────────

-- USUARIO: perfil_enum → VARCHAR
ALTER TABLE usuario
    ALTER COLUMN perfil TYPE VARCHAR(50) USING perfil::VARCHAR;

-- CHAMADO: remover defaults que dependem dos tipos enum ANTES de converter
ALTER TABLE chamado
    ALTER COLUMN status_atual DROP DEFAULT;
ALTER TABLE chamado
    ALTER COLUMN origem DROP DEFAULT;
ALTER TABLE chamado
    ALTER COLUMN categoria DROP DEFAULT;

-- CHAMADO: converter colunas
ALTER TABLE chamado
    ALTER COLUMN status_atual TYPE VARCHAR(50) USING status_atual::VARCHAR;
ALTER TABLE chamado
    ALTER COLUMN categoria TYPE VARCHAR(50) USING categoria::VARCHAR;
ALTER TABLE chamado
    ALTER COLUMN origem TYPE VARCHAR(50) USING origem::VARCHAR;

-- Restaurar o default de status_atual agora como VARCHAR simples
ALTER TABLE chamado
    ALTER COLUMN status_atual SET DEFAULT 'AGUARDANDO';

-- CHAMADO_STATUS_HISTORICO: chamado_status_enum → VARCHAR
ALTER TABLE chamado_status_historico
    ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR;

-- CHAT: remover default de tipo_midia antes de converter
ALTER TABLE chat
    ALTER COLUMN tipo_midia DROP DEFAULT;
ALTER TABLE chat
    ALTER COLUMN origem TYPE VARCHAR(50) USING origem::VARCHAR;
ALTER TABLE chat
    ALTER COLUMN tipo_midia TYPE VARCHAR(50) USING tipo_midia::VARCHAR;

-- Restaurar o default de tipo_midia agora como VARCHAR simples
ALTER TABLE chat
    ALTER COLUMN tipo_midia SET DEFAULT 'TEXTO';

-- Remover os tipos customizados com CASCADE para garantir
DROP TYPE IF EXISTS perfil_enum          CASCADE;
DROP TYPE IF EXISTS chamado_status_enum  CASCADE;
DROP TYPE IF EXISTS categoria_enum       CASCADE;
DROP TYPE IF EXISTS origem_enum          CASCADE;
DROP TYPE IF EXISTS chat_origem_enum     CASCADE;
DROP TYPE IF EXISTS tipo_midia_enum      CASCADE;