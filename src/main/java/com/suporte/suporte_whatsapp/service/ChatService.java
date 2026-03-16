package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.integration.ZApiClient;
import com.suporte.suporte_whatsapp.model.*;
import com.suporte.suporte_whatsapp.model.enums.*;
import com.suporte.suporte_whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChamadoRepository chamadoRepository;
    private final ChamadoStatusHistoricoRepository historicoRepository;
    private final SimpMessagingTemplate ws;
    private final ZApiClient zApiClient;

    @Value("${zapi.phone-number}")
    private String foneSuporte;

    // -------------------------------------------------------------------------
    // Fila plana (endpoint existente — sem alteração de contrato)
    // -------------------------------------------------------------------------

    public Page<ChatResponse> fila(Pageable pageable) {
        return chatRepository.findByChamadoIsNullOrderByDtEnvioAsc(pageable)
                .map(ChatResponse::from);
    }

    // -------------------------------------------------------------------------
    // Fila agrupada por contato — GET /api/chat/fila/agrupada
    // -------------------------------------------------------------------------

    /**
     * Retorna mensagens sem chamado agrupadas por contato.
     *
     * Estratégia de dois passos para evitar N+1:
     *
     * 1. Query principal com GROUP BY no banco — retorna contagens e
     * timestamps agregados para todos os contatos paginados.
     *
     * 2. Uma única query de preview busca a última mensagem de todos os
     * contatoIds da página de uma vez só (IN clause), e o resultado
     * é mergeado na memória via Map.
     *
     * Isso garante sempre exatamente 2 queries por chamada ao endpoint,
     * independente do tamanho da página.
     */
    public Page<FilaAgrupadaResponse> filaAgrupada(Pageable pageable) {
        Page<FilaAgrupadaResponse> pagina = chatRepository.findFilaAgrupada(pageable);

        if (pagina.isEmpty()) {
            return pagina;
        }

        // Coleta os IDs dos contatos desta página
        List<UUID> contatoIds = pagina.getContent().stream()
                .map(FilaAgrupadaResponse::getContatoId)
                .toList();

        // Uma única query para buscar o preview de todos os contatos da página
        Map<UUID, Object[]> previews = chatRepository
                .findUltimaMensagemPorContatos(contatoIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> row,
                        // Em caso de dtEnvio idêntico entre dois chats (raro mas possível),
                        // mantém o primeiro encontrado
                        (a, b) -> a));

        // Merge dos previews nos DTOs já paginados
        pagina.getContent().forEach(item -> {
            Object[] preview = previews.get(item.getContatoId());
            if (preview != null) {
                item.setUltimoTexto((String) preview[1]);
                item.setUltimoTipoMidia(preview[2] != null ? preview[2].toString() : null);
            }
        });

        return pagina;
    }

    // -------------------------------------------------------------------------
    // Por chamado
    // -------------------------------------------------------------------------

    public Page<ChatResponse> porChamado(UUID chamadoId, Pageable pageable) {
        return chatRepository.findByChamadoIdOrderByDtEnvioAsc(chamadoId, pageable)
                .map(ChatResponse::from);
    }

    // -------------------------------------------------------------------------
    // Resposta do analista
    // -------------------------------------------------------------------------

    @Transactional
    public ChatResponse responder(ChatRequest req, Usuario analista) {
        Chamado chamado = chamadoRepository.findById(req.getChamadoId())
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado"));

        Chat msg = Chat.builder()
                .chamado(chamado)
                .contato(chamado.getContato())
                .usuario(analista)
                .origem(ChatOrigem.SUPORTE)
                .dtEnvio(LocalDateTime.now())
                .texto(req.getTexto())
                .fileUrl(req.getFileUrl())
                .tipoMidia(req.getTipoMidia())
                .foneCliente(chamado.getContato().getTelefone())
                .foneSuporte(foneSuporte)
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

        // Enviar para o WhatsApp do cliente após commit da transação
        despacharParaWhatsApp(msg, chamado.getContato().getTelefone());

        ws.convertAndSend("/topic/mensagens", ChatResponse.from(msg));
        return ChatResponse.from(msg);
    }

    // -------------------------------------------------------------------------
    // Despacho para Z-API
    // -------------------------------------------------------------------------

    private void despacharParaWhatsApp(Chat msg, String foneDestino) {
        try {
            switch (msg.getTipoMidia()) {
                case TEXTO -> zApiClient.enviarTexto(foneDestino, msg.getTexto());
                case IMAGEM -> zApiClient.enviarImagem(foneDestino, msg.getFileUrl(), msg.getTexto());
                case AUDIO -> zApiClient.enviarAudio(foneDestino, msg.getFileUrl());
                case VIDEO -> zApiClient.enviarVideo(foneDestino, msg.getFileUrl(), msg.getTexto());
            }
        } catch (Exception e) {
            // Falha na Z-API nunca deve reverter a transação já commitada.
            // A mensagem já está salva no banco; o analista pode reenviar manualmente.
            log.error("[Chat] Falha ao enviar mensagem via Z-API para número terminado em ...{}. msgId={} | erro={}",
                    foneDestino.length() > 4 ? foneDestino.substring(foneDestino.length() - 4) : "????",
                    msg.getId(),
                    e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de histórico
    // -------------------------------------------------------------------------

    private void abrirHistorico(Chamado chamado, ChamadoStatus status, Usuario u) {
        historicoRepository.save(ChamadoStatusHistorico.builder()
                .chamado(chamado)
                .status(status)
                .dtInicio(LocalDateTime.now())
                .usuarioResponsavel(u)
                .build());
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