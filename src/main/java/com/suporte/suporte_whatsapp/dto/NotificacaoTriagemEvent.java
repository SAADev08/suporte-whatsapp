package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.NivelSla;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento emitido via WebSocket quando um contato na triagem ultrapassa
 * o tempo de espera sem receber resposta do suporte.
 *
 * Thresholds:
 * <ul>
 *   <li>{@code ALERTA}  — entre 10 e 18 minutos sem resposta</li>
 *   <li>{@code ESCALADO} — acima de 18 minutos sem resposta (exibir como URGENTE no frontend)</li>
 * </ul>
 *
 * Tópico WebSocket: {@code /topic/notificacoes-triagem}
 * Endpoint REST de recuperação: {@code GET /api/sla/triagem/ativos}
 */
@Data
@AllArgsConstructor
public class NotificacaoTriagemEvent {
    private UUID contatoId;
    private String nomeContato;
    private NivelSla nivel;
    private String mensagem;
    private LocalDateTime timestamp;
}
