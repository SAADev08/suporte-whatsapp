package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.NotificacaoSlaEvent;
import com.suporte.suporte_whatsapp.dto.NotificacaoTriagemEvent;
import com.suporte.suporte_whatsapp.dto.WsEnvelope;
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
import java.util.UUID;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SlaScheduler {

    private final ChatRepository chatRepository;
    private final SimpMessagingTemplate ws;
    private final SlaNotificacaoCache slaCache;
    private final TriagemNotificacaoCache triagemCache;

    private static final int TRIAGEM_ALERTA_MIN = 10;
    private static final int TRIAGEM_URGENTE_MIN = 18;

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

            UUID chamadoId = chat.getChamado().getId();
            long minutos = java.time.Duration.between(chat.getDtEnvio(), agora).toMinutes();

            NivelSla nivel = minutos >= escaladoMin ? NivelSla.ESCALADO
                    : minutos >= criticoMin ? NivelSla.CRITICO
                            : NivelSla.ALERTA;

            if (slaCache.jaNotificadoNesseNivel(chamadoId, nivel)) {
                log.debug("[SLA] Chamado {} - {} já notificado, suprimindo", chamadoId, nivel);
                continue;
            }

            String msg = String.format("Chamado #%S sem resposta há %d min.", chamadoId, minutos);

            ws.convertAndSend("/topic/notificacoes",
                    WsEnvelope.of(new NotificacaoSlaEvent(chamadoId, nivel, msg, agora)));

            slaCache.registrar(chamadoId, nivel, msg);

            log.info("[SLA] Chamado {} – {} ({} min)", chamadoId, nivel, minutos);
        }

        verificarSlaTriagem(agora);
    }

    private void verificarSlaTriagem(LocalDateTime agora) {
        List<Chat> pendentes = chatRepository.findMensagensTriagemSemResposta(
                agora.minusMinutes(TRIAGEM_ALERTA_MIN));

        for (Chat chat : pendentes) {
            UUID contatoId = chat.getContato().getId();
            String nomeContato = chat.getContato().getNome();
            long minutos = java.time.Duration.between(chat.getDtEnvio(), agora).toMinutes();

            NivelSla nivel = minutos >= TRIAGEM_URGENTE_MIN ? NivelSla.ESCALADO : NivelSla.ALERTA;

            if (triagemCache.jaNotificadoNesseNivel(contatoId, nivel)) {
                log.debug("[SLA-Triagem] Contato {} - {} já notificado, suprimindo", contatoId, nivel);
                continue;
            }

            String msg = String.format("Contato '%s' aguarda resposta na triagem há %d min.", nomeContato, minutos);

            ws.convertAndSend("/topic/notificacoes-triagem",
                    WsEnvelope.of(new NotificacaoTriagemEvent(contatoId, nomeContato, nivel, msg, agora)));

            triagemCache.registrar(contatoId, nomeContato, nivel, msg);

            log.info("[SLA-Triagem] Contato {} – {} ({} min)", contatoId, nivel, minutos);
        }
    }
}