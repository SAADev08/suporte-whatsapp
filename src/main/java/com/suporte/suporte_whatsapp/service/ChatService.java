package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.*;
import com.suporte.suporte_whatsapp.model.enums.*;
import com.suporte.suporte_whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChamadoRepository chamadoRepository;
    private final ChamadoStatusHistoricoRepository historicoRepository;
    private final SimpMessagingTemplate ws;

    public List<ChatResponse> fila() {
        return chatRepository.findByChamadoIsNullOrderByDtEnvioAsc()
                .stream().map(ChatResponse::from).toList();
    }

    public List<ChatResponse> porChamado(UUID chamadoId) {
        return chatRepository.findByChamadoIdOrderByDtEnvioAsc(chamadoId)
                .stream().map(ChatResponse::from).toList();
    }

    @Transactional
    public ChatResponse responder(ChatRequest req, Usuario analista) {
        Chamado chamado = chamadoRepository.findById(req.getChamadoId())
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado"));

        Chat msg = Chat.builder()
                .chamado(chamado).contato(chamado.getContato()).usuario(analista)
                .origem(ChatOrigem.SUPORTE).dtEnvio(LocalDateTime.now())
                .texto(req.getTexto()).fileUrl(req.getFileUrl()).tipoMidia(req.getTipoMidia())
                .foneCliente(chamado.getContato().getTelefone())
                .nomeContato(chamado.getContato().getNome())
                .build();
        msg = chatRepository.save(msg);

        if (chamado.getDtPrimeiraResposta() == null) {
            chamado.setDtPrimeiraResposta(LocalDateTime.now());
            if (chamado.getUsuarioResponsavel() == null)
                chamado.setUsuarioResponsavel(analista);
            chamado.setStatusAtual(ChamadoStatus.EM_ATENDIMENTO);
            chamadoRepository.save(chamado);
            fecharHistoricoAtual(chamado.getId());
            abrirHistorico(chamado, ChamadoStatus.EM_ATENDIMENTO, analista);
            ws.convertAndSend("/topic/chamados/" + chamado.getId(), ChamadoResponse.from(chamado));
        }

        ws.convertAndSend("/topic/mensagens", ChatResponse.from(msg));
        return ChatResponse.from(msg);
    }

    private void abrirHistorico(Chamado chamado, ChamadoStatus status, Usuario u) {
        historicoRepository.save(ChamadoStatusHistorico.builder()
                .chamado(chamado).status(status)
                .dtInicio(LocalDateTime.now()).usuarioResponsavel(u).build());
    }

    private void fecharHistoricoAtual(UUID chamadoId) {
        historicoRepository.findByChamadoIdAndDtFimIsNull(chamadoId).ifPresent(h -> {
            LocalDateTime agora = LocalDateTime.now();
            h.setDtFim(agora);
            h.setTempoEmStatusSegundos(ChronoUnit.SECONDS.between(h.getDtInicio(), agora));
            historicoRepository.save(h);
        });
    }
}