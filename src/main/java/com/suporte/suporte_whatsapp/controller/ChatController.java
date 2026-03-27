package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.Usuario;
import com.suporte.suporte_whatsapp.service.ChatService;
import com.suporte.suporte_whatsapp.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService service;
    private final UsuarioService usuarioService;

    @GetMapping("/fila")
    public ResponseEntity<Page<ChatResponse>> fila(
            @PageableDefault(size = 30, sort = "dtEnvio", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.fila(pageable));
    }

    /**
     * Fila agrupada por contato — endpoint principal para a tela de triagem.
     *
     * Cada item representa um contato com mensagens pendentes, contendo:
     * - identificação do contato (id, nome, telefone)
     * - flag pendenteVinculacao: se TRUE, vincular a Cliente antes de criar chamado
     * - totalMensagens: quantas mensagens aguardam triagem
     * - dtPrimeiraMensagem: quando chegou a primeira — usado para cálculo de SLA
     * - dtUltimaMensagem: quando chegou a mais recente
     * - ultimoTexto / ultimoTipoMidia: preview para exibição na lista
     *
     * Ordenação padrão: contato aguardando há mais tempo primeiro (maior urgência).
     *
     * Exemplo de uso:
     * GET /api/chat/fila/agrupada?page=0&size=20
     */
    @GetMapping("/fila/agrupada")
    public ResponseEntity<Page<FilaAgrupadaResponse>> filaAgrupada(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.filaAgrupada(pageable));
    }

    /**
     * Timeline unificada de um contato — todas as mensagens em ordem cronológica,
     * independente do chamado ao qual pertencem (ou sem chamado).
     *
     * O campo {@code chamadoId} em cada item permite ao frontend renderizar
     * marcadores visuais de abertura/encerramento de chamado dentro da conversa.
     *
     * GET /api/chat/contato/{contatoId}?page=0&size=50
     */
    @GetMapping("/contato/{contatoId}")
    public ResponseEntity<Page<ChatResponse>> conversaContato(
            @PathVariable UUID contatoId,
            @PageableDefault(size = 50, sort = "dtEnvio", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.conversaContato(contatoId, pageable));
    }

    @GetMapping("/chamado/{chamadoId}")
    public ResponseEntity<Page<ChatResponse>> porChamado(
            @PathVariable UUID chamadoId,
            @PageableDefault(size = 30, sort = "dtEnvio", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.porChamado(chamadoId, pageable));
    }

    /**
     * Retorna todas as mensagens de um contato na triagem (sem chamado),
     * incluindo respostas enviadas pelo suporte diretamente na triagem.
     *
     * GET /api/chat/triagem/{contatoId}
     */
    @GetMapping("/triagem/{contatoId}")
    public ResponseEntity<List<ChatResponse>> conversaTriagem(@PathVariable UUID contatoId) {
        return ResponseEntity.ok(service.conversaTriagem(contatoId));
    }

    /**
     * Responde a um contato diretamente na triagem, sem abrir chamado.
     * A mensagem fica em CHAT com id_chamado = NULL e é enviada via Z-API.
     *
     * POST /api/chat/responder-triagem
     */
    @PostMapping("/responder-triagem")
    public ResponseEntity<ChatResponse> responderTriagem(
            @Valid @RequestBody RespostaTriagemRequest req,
            Authentication auth) {
        Usuario analista = usuarioService.findByEmailOrThrow(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.responderTriagem(req, analista));
    }

    @PostMapping("/responder")
    public ResponseEntity<ChatResponse> responder(@Valid @RequestBody ChatRequest req,
            Authentication auth) {
        Usuario analista = usuarioService.findByEmailOrThrow(auth.getName());
        return ResponseEntity.ok(service.responder(req, analista));
    }

    @PostMapping("/iniciar")
    public ResponseEntity<IniciarChatResponse> iniciar(@Valid @RequestBody IniciarChatRequest req,
            Authentication auth) {
        Usuario analista = usuarioService.findByEmailOrThrow(auth.getName());
        IniciarChatResponse response = service.iniciarChat(req, analista);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}