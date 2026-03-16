-- ─────────────────────────────────────────────────────────────────────────────
-- V3__add_chat_message_id.sql
--
-- Adiciona o campo `message_id` na tabela chat para garantir idempotência
-- no processamento de webhooks da Z-API.
--
-- Contexto:
--   A Z-API pode reenviar o mesmo webhook em caso de timeout ou falha de
--   entrega (comportamento padrão de sistemas de mensageria). Sem controle
--   de idempotência, cada retentativa resultaria em uma mensagem duplicada
--   no banco de dados.
--
--   O campo `message_id` armazena o ID único da mensagem fornecido pela
--   Z-API (campo "messageId" no payload). A constraint UNIQUE garante que
--   a segunda tentativa de inserir o mesmo messageId seja rejeitada no
--   nível do banco — a camada mais segura para esta garantia.
--
-- Comportamento no WebhookService:
--   Antes de persistir, verificar se já existe um Chat com o mesmo
--   message_id. Se existir, descartar silenciosamente (não é erro).
--
-- Por que nullable:
--   Mensagens enviadas pelos analistas (origem=SUPORTE) não têm messageId
--   da Z-API — são geradas internamente. A constraint UNIQUE é parcial
--   (WHERE message_id IS NOT NULL) para não conflitar com NULLs múltiplos.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE chat
    ADD COLUMN message_id VARCHAR(255);

-- Índice único parcial: aplica unicidade apenas onde message_id está preenchido.
-- Múltiplos NULLs são permitidos (mensagens do analista não têm messageId).
CREATE UNIQUE INDEX uq_chat_message_id
    ON chat (message_id)
    WHERE message_id IS NOT NULL;

COMMENT ON COLUMN chat.message_id
    IS 'ID único da mensagem fornecido pela Z-API. Usado para garantir idempotência no processamento de webhooks. NULL para mensagens enviadas pelos analistas.';
