package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.NotificacaoSlaEvent;
import com.suporte.suporte_whatsapp.model.Chat;
import com.suporte.suporte_whatsapp.model.enums.NivelSla;
import com.suporte.suporte_whatsapp.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SlaScheduler {

    private final ChatRepository chatRepository;
    private final SimpMessagingTemplate ws;

    @Value("${sla.alerta-minutos}")
    private int alertaMin;
    @Value("${sla.critico-minutos}")
    private int criticoMin;
    @Value("${sla.escalado-minutos}")
    private int escaladoMin;

    @Scheduled(fixedDelay = 60_000)
    public void verificarSla() {
        LocalDateTime agora = LocalDateTime.now();
        List<Chat> pendentes = chatRepository.findMensagensSemResposta(
                agora.minusMinutes(alertaMin));

        for (Chat chat : pendentes) {
            if (chat.getChamado() == null)
                continue;
            long minutos = java.time.Duration.between(chat.getDtEnvio(), agora).toMinutes();

            NivelSla nivel = minutos >= escaladoMin ? NivelSla.ESCALADO
                    : minutos >= criticoMin ? NivelSla.CRITICO
                            : NivelSla.ALERTA;

            ws.convertAndSend("/topic/notificacoes", new NotificacaoSlaEvent(
                    chat.getChamado().getId(), nivel,
                    "Mensagem sem resposta há " + minutos + " min. Chamado #" + chat.getChamado().getId(),
                    agora));

            log.info("[SLA] Chamado {} – {} ({} min)", chat.getChamado().getId(), nivel, minutos);
        }
    }
}