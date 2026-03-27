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
    private final ContatoRepository contatoRepository;
    private final SimpMessagingTemplate ws;
    private final ZApiClient zApiClient;
    private final SlaNotificacaoCache slaCache;
    private final TriagemNotificacaoCache triagemCache;

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
    // Conversa unificada por contato — GET /api/chat/contato/{contatoId}
    // -------------------------------------------------------------------------

    /**
     * Retorna todas as mensagens de um contato em ordem cronológica,
     * incluindo mensagens de triagem (sem chamado) e mensagens de todos os
     * chamados já abertos para esse contato.
     *
     * O campo {@code chamadoId} em cada {@link ChatResponse} permite ao
     * frontend identificar os limites de cada chamado e renderizar marcadores
     * visuais (abertura/encerramento) dentro da timeline contínua.
     */
    public Page<ChatResponse> conversaContato(UUID contatoId, Pageable pageable) {
        return chatRepository.findByContatoIdOrderByDtEnvioAsc(contatoId, pageable)
                .map(ChatResponse::from);
    }

    // -------------------------------------------------------------------------
    // Por chamado
    // -------------------------------------------------------------------------

    public Page<ChatResponse> porChamado(UUID chamadoId, Pageable pageable) {
        return chatRepository.findByChamadoIdOrderByDtEnvioAsc(chamadoId, pageable)
                .map(ChatResponse::from);
    }

    // -------------------------------------------------------------------------
    // Conversa de triagem — GET /api/chat/triagem/{contatoId}
    // -------------------------------------------------------------------------

    /**
     * Retorna todas as mensagens (cliente + suporte) de um contato que ainda
     * não possuem chamado vinculado. Usado para visualizar a conversa na
     * tela de triagem antes de abrir um chamado formal.
     */
    public List<ChatResponse> conversaTriagem(UUID contatoId) {
        return chatRepository
                .findByContatoIdAndChamadoIsNullOrderByDtEnvioAsc(contatoId)
                .stream()
                .map(ChatResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Resposta na triagem — POST /api/chat/responder-triagem
    // -------------------------------------------------------------------------

    /**
     * Permite ao analista responder a um contato diretamente na triagem,
     * sem precisar abrir um chamado formal.
     *
     * A mensagem é salva em CHAT com id_chamado = NULL e enviada via Z-API.
     * Quando o chamado for criado posteriormente, todas essas mensagens
     * (incluindo esta resposta) serão vinculadas a ele.
     */
    @Transactional
    public ChatResponse responderTriagem(RespostaTriagemRequest req, Usuario analista) {
        Contato contato = contatoRepository.findById(req.getContatoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contato não encontrado: " + req.getContatoId()));

        if (!contato.getAtivo()) {
            throw new IllegalStateException("Não é possível responder a contato inativo.");
        }

        Chat msg = Chat.builder()
                .chamado(null) // sem chamado — permanece na triagem
                .contato(contato)
                .usuario(analista)
                .origem(ChatOrigem.SUPORTE)
                .dtEnvio(LocalDateTime.now())
                .texto(req.getTexto())
                .fileUrl(req.getFileUrl())
                .tipoMidia(req.getTipoMidia())
                .foneCliente(contato.getTelefone())
                .foneSuporte(foneSuporte)
                .nomeContato(contato.getNome())
                .build();

        msg = chatRepository.save(msg);

        triagemCache.limpar(contato.getId());
        despacharParaWhatsApp(msg, contato.getTelefone(), analista);

        // Notifica os frontends via WebSocket (tópico geral e timeline do contato)
        ChatResponse chatResponse = ChatResponse.from(msg);
        ws.convertAndSend("/topic/mensagens", WsEnvelope.of(chatResponse));
        ws.convertAndSend("/topic/contato/" + contato.getId(), WsEnvelope.of(chatResponse));

        log.info("[Triagem] Resposta enviada pelo analista '{}' para contato '{}' (tel: ...{}) sem chamado.",
                analista.getEmail(),
                contato.getNome(),
                contato.getTelefone().length() > 4
                        ? contato.getTelefone().substring(contato.getTelefone().length() - 4)
                        : "????");

        return chatResponse;
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

        slaCache.limpar(chamado.getId());

        if (chamado.getDtPrimeiraResposta() == null) {
            chamado.setDtPrimeiraResposta(LocalDateTime.now());
            if (chamado.getUsuarioResponsavel() == null)
                chamado.setUsuarioResponsavel(analista);
            chamado.setStatusAtual(ChamadoStatus.EM_ATENDIMENTO);
            chamadoRepository.save(chamado);
            fecharHistoricoAtual(chamado.getId());
            abrirHistorico(chamado, ChamadoStatus.EM_ATENDIMENTO, analista);
            ws.convertAndSend("/topic/chamados/" + chamado.getId(), WsEnvelope.of(ChamadoResponse.from(chamado)));
        }

        // Enviar para o WhatsApp do cliente após commit da transação
        despacharParaWhatsApp(msg, chamado.getContato().getTelefone(), analista);

        ChatResponse chatResponseResponder = ChatResponse.from(msg);
        ws.convertAndSend("/topic/mensagens", WsEnvelope.of(chatResponseResponder));
        ws.convertAndSend("/topic/contato/" + chamado.getContato().getId(), WsEnvelope.of(chatResponseResponder));
        return chatResponseResponder;
    }

    // -------------------------------------------------------------------------
    // Iniciar conversa ativa (outbound) — POST /api/chat/iniciar
    // -------------------------------------------------------------------------

    /**
     * Inicia uma conversa ativa a partir do suporte.
     *
     * <p>
     * Fluxo completo em transação única:
     * <ol>
     * <li>Valida o contato (deve existir, estar ativo e sem pendência de
     * vinculação).</li>
     * <li>Cria um novo {@link Chamado} com origem=WHATSAPP, status=EM_ATENDIMENTO,
     * preenchendo dt_abertura, dt_primeira_mensagem e dt_primeira_resposta
     * com o instante atual — pois o suporte já está atendendo.</li>
     * <li>Registra o histórico de status inicial.</li>
     * <li>Salva a primeira mensagem em {@link Chat} com origem=SUPORTE.</li>
     * <li>Envia a mensagem via Z-API.</li>
     * <li>Notifica todos os frontends via WebSocket.</li>
     * </ol>
     *
     * @param req      dados do contato, mensagem e chamado a criar
     * @param analista usuário autenticado que está iniciando a conversa
     * @return chamado criado + primeira mensagem enviada
     */

    @Transactional
    public IniciarChatResponse iniciarChat(IniciarChatRequest req, Usuario analista) {

        // 1. Validar contato
        Contato contato = contatoRepository.findById(req.getContatoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contato não encontrado: " + req.getContatoId()));

        if (!contato.getAtivo()) {
            throw new IllegalStateException(
                    "Não é possível iniciar conversa com contato inativo.");
        }

        if (contato.getPendenteVinculacao()) {
            throw new IllegalStateException(
                    "Contato ainda não está vinculado a um cliente. "
                            + "Realize a vinculação antes de iniciar a conversa.");
        }

        // 2. Criar chamado já em atendimento
        LocalDateTime agora = LocalDateTime.now();
        String descricao = (req.getDescricaoChamado() != null && !req.getDescricaoChamado().isBlank())
                ? req.getDescricaoChamado()
                : req.getTexto();

        Chamado chamado = Chamado.builder()
                .texto(descricao)
                .categoria(req.getCategoria() != null ? req.getCategoria() : Categoria.DUVIDA)
                .statusAtual(ChamadoStatus.EM_ATENDIMENTO)
                .origem(Origem.WHATSAPP)
                .dtAbertura(agora)
                .dtPrimeiraMensagem(agora)
                .dtPrimeiraResposta(agora) // suporte iniciou — já conta como primeira resposta
                .contato(contato)
                .usuarioResponsavel(analista)
                .build();

        chamado = chamadoRepository.save(chamado);

        // 3. Histórico de status
        abrirHistorico(chamado, ChamadoStatus.EM_ATENDIMENTO, analista);

        slaCache.limpar(chamado.getId());

        // 4. Salvar primeira mensagem
        Chat msg = Chat.builder()
                .chamado(chamado)
                .contato(contato)
                .usuario(analista)
                .origem(ChatOrigem.SUPORTE)
                .dtEnvio(agora)
                .texto(req.getTexto())
                .fileUrl(req.getFileUrl())
                .tipoMidia(req.getTipoMidia())
                .foneCliente(contato.getTelefone())
                .foneSuporte(foneSuporte)
                .nomeContato(contato.getNome())
                .build();

        msg = chatRepository.save(msg);

        // 5. Enviar via Z-API (fora da transação, mas ainda dentro do método —
        // falha de envio NÃO reverte o chamado já criado)
        despacharParaWhatsApp(msg, contato.getTelefone(), analista);

        // 6. Notificar frontends
        ChamadoResponse chamadoResponse = ChamadoResponse.from(chamado);
        ChatResponse chatResponse = ChatResponse.from(msg);

        ws.convertAndSend("/topic/chamados/novo", WsEnvelope.of(chamadoResponse));
        ws.convertAndSend("/topic/chamados/" + chamado.getId(), WsEnvelope.of(chamadoResponse));
        ws.convertAndSend("/topic/mensagens", WsEnvelope.of(chatResponse));
        ws.convertAndSend("/topic/contato/" + contato.getId(), WsEnvelope.of(chatResponse));

        log.info("[Chat] Conversa ativa iniciada pelo analista '{}' para contato '{}' (tel: ...{}). chamadoId={}",
                analista.getEmail(),
                contato.getNome(),
                contato.getTelefone().length() > 4
                        ? contato.getTelefone().substring(contato.getTelefone().length() - 4)
                        : "????",
                chamado.getId());

        return IniciarChatResponse.builder()
                .chamado(chamadoResponse)
                .mensagem(chatResponse)
                .build();
    }
    // -------------------------------------------------------------------------
    // Despacho para Z-API
    // -------------------------------------------------------------------------

    private void despacharParaWhatsApp(Chat msg, String foneDestino, Usuario analista) {
        try {
            String label = analista.getPerfil() == Perfil.GESTOR ? "Gestor" : "Suporte";
            String assinatura = analista.getNome() + " | " + label;

            switch (msg.getTipoMidia()) {
                case TEXTO -> {
                    String textoComAssinatura = assinatura + ": " + msg.getTexto();
                    zApiClient.enviarTexto(foneDestino, textoComAssinatura);
                }
                case IMAGEM -> {
                    String captionAssinada = assinatura + ": "
                            + (msg.getTexto() != null ? msg.getTexto() : "");
                    zApiClient.enviarImagem(foneDestino, msg.getFileUrl(), captionAssinada);
                }
                case AUDIO -> zApiClient.enviarAudio(foneDestino, msg.getFileUrl());
                case VIDEO -> {
                    String captionAssinada = assinatura + ": "
                            + (msg.getTexto() != null ? msg.getTexto() : "");
                    zApiClient.enviarVideo(foneDestino, msg.getFileUrl(), captionAssinada);
                }
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