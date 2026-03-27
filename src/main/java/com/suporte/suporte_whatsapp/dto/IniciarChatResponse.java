package com.suporte.suporte_whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta do endpoint POST /api/chat/iniciar.
 *
 * Retorna o chamado criado e a primeira mensagem enviada,
 * permitindo que o frontend abra diretamente a tela de atendimento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IniciarChatResponse {

    /** Chamado criado automaticamente para esta conversa ativa. */
    private ChamadoResponse chamado;

    /** Primeira mensagem enviada ao contato. */
    private ChatResponse mensagem;
}