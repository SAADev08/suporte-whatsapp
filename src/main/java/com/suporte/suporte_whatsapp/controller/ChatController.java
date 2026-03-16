package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.Usuario;
import com.suporte.suporte_whatsapp.repository.UsuarioRepository;
import com.suporte.suporte_whatsapp.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService service;
    private final UsuarioRepository usuarioRepository;

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

    @GetMapping("/chamado/{chamadoId}")
    public ResponseEntity<Page<ChatResponse>> porChamado(
            @PathVariable UUID chamadoId,
            @PageableDefault(size = 30, sort = "dtEnvio", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.porChamado(chamadoId, pageable));
    }

    @PostMapping("/responder")
    public ResponseEntity<ChatResponse> responder(@Valid @RequestBody ChatRequest req,
            Authentication auth) {
        Usuario analista = usuarioRepository.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(service.responder(req, analista));
    }
}