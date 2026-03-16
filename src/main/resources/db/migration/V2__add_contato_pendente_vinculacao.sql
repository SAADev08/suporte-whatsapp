-- ─────────────────────────────────────────────────────────────────────────────
-- V2__add_contato_pendente_vinculacao.sql
--
-- Adiciona a flag `pendente_vinculacao` na tabela contato.
--
-- Contexto:
--   Quando uma mensagem chega via webhook de um número desconhecido,
--   o sistema cria o Contato automaticamente para não perder a mensagem.
--   Porém, esse contato fica sem vínculo com nenhum Cliente — violando
--   a regra de negócio RF02/RF03.
--
--   A flag `pendente_vinculacao = TRUE` sinaliza que este contato foi
--   criado automaticamente via webhook e ainda aguarda que um analista
--   o vincule a pelo menos um Cliente antes de abrir um chamado.
--
-- Distinção semântica (importante):
--   ativo = FALSE  → contato foi INATIVADO manualmente por um analista.
--   pendente_vinculacao = TRUE → contato foi criado via webhook e ainda
--                                não tem vínculo com Cliente.
--   Os dois campos são independentes e não devem ser confundidos.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE contato
    ADD COLUMN pendente_vinculacao BOOLEAN NOT NULL DEFAULT FALSE;

-- Índice parcial: otimiza a query GET /api/contatos/pendentes que filtra
-- exclusivamente por pendente_vinculacao = TRUE. Como a grande maioria dos
-- contatos terá FALSE, o índice parcial é menor e mais eficiente que um
-- índice convencional.
CREATE INDEX idx_contato_pendente
    ON contato (created_at DESC)
    WHERE pendente_vinculacao = TRUE;

COMMENT ON COLUMN contato.pendente_vinculacao
    IS 'TRUE quando o contato foi criado automaticamente via webhook e ainda não possui vínculo com nenhum Cliente.';
