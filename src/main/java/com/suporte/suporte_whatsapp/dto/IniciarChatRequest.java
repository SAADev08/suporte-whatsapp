package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.Categoria;
import com.suporte.suporte_whatsapp.model.enums.TipoMidia;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

/**
 * Payload para iniciar uma conversa ativa pelo suporte.
 *
 * Fluxo: o analista seleciona (ou cadastra) um contato e envia a primeira
 * mensagem. O sistema cria automaticamente um Chamado com origem=WHATSAPP,
 * status=EM_ATENDIMENTO, preenche dt_primeira_resposta e envia a mensagem
 * via Z-API.
 *
 * Campos opcionais (texto/fileUrl/tipoMidia) seguem o mesmo contrato do
 * ChatRequest existente para manter consistência no frontend.
 */
@Data
public class IniciarChatRequest {

    /**
     * Contato destinatário. Deve estar ativo e vinculado a pelo menos um cliente.
     */
    @NotNull(message = "contatoId é obrigatório")
    private UUID contatoId;

    /**
     * Descrição inicial do chamado (opcional).
     * Se não informado, usa o próprio texto da mensagem.
     */
    private String descricaoChamado;

    /** Categoria do chamado (DUVIDA por padrão se não informada). */
    private Categoria categoria;

    // ── Mensagem ──────────────────────────────────────────────────────────────

    /** Texto da mensagem. Obrigatório quando tipoMidia=TEXTO. */
    private String texto;

    /** URL da mídia. Obrigatório quando tipoMidia != TEXTO. */
    private String fileUrl;

    /** Tipo de mídia. Padrão: TEXTO. */
    private TipoMidia tipoMidia = TipoMidia.TEXTO;
}