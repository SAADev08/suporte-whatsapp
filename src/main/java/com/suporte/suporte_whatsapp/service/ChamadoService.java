package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.*;
import com.suporte.suporte_whatsapp.model.enums.*;
import com.suporte.suporte_whatsapp.repository.*;
import com.suporte.suporte_whatsapp.specification.ChamadoSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final ChamadoStatusHistoricoRepository historicoRepository;
    private final ChatRepository chatRepository;
    private final ContatoService contatoService;
    private final UsuarioService usuarioService;
    private final SubtipoRepository subtipoRepository;
    private final SimpMessagingTemplate ws;

    public Page<ChamadoResponse> listar(ChamadoStatus status, Origem origem,
            UUID contatoId, UUID usuarioId, Pageable pageable) {
        return chamadoRepository
                .findAll(ChamadoSpecification.comFiltros(status, origem, contatoId, usuarioId), pageable)
                .map(ChamadoResponse::from);
    }

    public ChamadoResponse buscarPorId(UUID id) {
        Chamado ch = findOrThrow(id);
        ChamadoResponse r = ChamadoResponse.from(ch);
        r.setHistorico(historicoRepository.findByChamadoIdOrderByDtInicioAsc(id)
                .stream().map(ChamadoStatusHistoricoResponse::from).toList());
        return r;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest req) {
        Contato contato = contatoService.findOrThrow(req.getContatoId());
        Usuario responsavel = req.getUsuarioResponsavelId() != null
                ? usuarioService.findOrThrow(req.getUsuarioResponsavelId())
                : null;
        Subtipo subtipo = req.getSubtipoId() != null
                ? subtipoRepository.findById(req.getSubtipoId())
                        .orElseThrow(() -> new IllegalArgumentException("Subtipo não encontrado"))
                : null;

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime dtPrimeiraMensagem = agora;
        if (req.getChatIds() != null && !req.getChatIds().isEmpty()) {
            dtPrimeiraMensagem = chatRepository.findAllById(req.getChatIds()).stream()
                    .map(Chat::getDtEnvio).min(LocalDateTime::compareTo).orElse(agora);
        }

        Chamado chamado = Chamado.builder()
                .texto(req.getTexto()).categoria(req.getCategoria())
                .statusAtual(ChamadoStatus.AGUARDANDO).origem(req.getOrigem())
                .dtAbertura(agora).dtPrimeiraMensagem(dtPrimeiraMensagem)
                .contato(contato).usuarioResponsavel(responsavel).subtipo(subtipo)
                .build();
        chamado = chamadoRepository.save(chamado);

        if (req.getChatIds() != null && !req.getChatIds().isEmpty()) {
            final Chamado c = chamado;
            chatRepository.findAllById(req.getChatIds()).forEach(msg -> {
                msg.setChamado(c);
                chatRepository.save(msg);
            });
        }

        abrirHistorico(chamado, ChamadoStatus.AGUARDANDO, responsavel);
        ws.convertAndSend("/topic/chamados/" + chamado.getId(), ChamadoResponse.from(chamado));
        return ChamadoResponse.from(chamado);
    }

    @Transactional
    public ChamadoResponse atualizar(UUID id, ChamadoRequest req) {
        Chamado ch = findOrThrow(id);
        ChamadoStatus statusAnterior = ch.getStatusAtual();

        if (req.getUsuarioResponsavelId() != null)
            ch.setUsuarioResponsavel(usuarioService.findOrThrow(req.getUsuarioResponsavelId()));
        if (req.getSubtipoId() != null)
            ch.setSubtipo(subtipoRepository.findById(req.getSubtipoId()).orElse(null));
        if (req.getCategoria() != null)
            ch.setCategoria(req.getCategoria());
        if (req.getTexto() != null)
            ch.setTexto(req.getTexto());
        if (req.getSolucao() != null)
            ch.setSolucao(req.getSolucao());

        if (req.getStatusAtual() != null && req.getStatusAtual() != statusAnterior) {
            fecharHistoricoAtual(id);
            ch.setStatusAtual(req.getStatusAtual());
            abrirHistorico(ch, req.getStatusAtual(), ch.getUsuarioResponsavel());
        }

        ch = chamadoRepository.save(ch);
        ws.convertAndSend("/topic/chamados/" + ch.getId(), ChamadoResponse.from(ch));
        return ChamadoResponse.from(ch);
    }

    @Transactional
    public ChamadoResponse encerrar(UUID id, String solucao) {
        Chamado ch = findOrThrow(id);
        if (solucao == null || solucao.isBlank())
            throw new IllegalArgumentException("Campo 'solucao' é obrigatório para encerrar o chamado");
        if (ch.getStatusAtual() == ChamadoStatus.ENCERRADO)
            throw new IllegalStateException("Chamado já está encerrado");

        LocalDateTime agora = LocalDateTime.now();
        fecharHistoricoAtual(id);
        ch.setStatusAtual(ChamadoStatus.ENCERRADO);
        ch.setSolucao(solucao);
        ch.setDtEncerramento(agora);
        ch.setTempoTotalSegundos(ChronoUnit.SECONDS.between(ch.getDtAbertura(), agora));
        abrirHistorico(ch, ChamadoStatus.ENCERRADO, ch.getUsuarioResponsavel());
        fecharHistoricoAtual(id);

        ch = chamadoRepository.save(ch);
        ws.convertAndSend("/topic/chamados/" + ch.getId(), ChamadoResponse.from(ch));
        return ChamadoResponse.from(ch);
    }

    public List<ChamadoStatusHistoricoResponse> buscarHistorico(UUID id) {
        return historicoRepository.findByChamadoIdOrderByDtInicioAsc(id)
                .stream().map(ChamadoStatusHistoricoResponse::from).toList();
    }

    private void abrirHistorico(Chamado chamado, ChamadoStatus status, Usuario responsavel) {
        historicoRepository.save(ChamadoStatusHistorico.builder()
                .chamado(chamado).status(status)
                .dtInicio(LocalDateTime.now()).usuarioResponsavel(responsavel)
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

    public Chamado findOrThrow(UUID id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado: " + id));
    }
}