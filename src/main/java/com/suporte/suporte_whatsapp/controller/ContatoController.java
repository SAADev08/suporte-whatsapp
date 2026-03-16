package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.service.ContatoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/contatos")
@RequiredArgsConstructor
public class ContatoController {

    private final ContatoService service;

    @GetMapping
    public ResponseEntity<Page<ContatoResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.listar(nome, telefone, email, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContatoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    /**
     * Lista contatos criados automaticamente via webhook que ainda não
     * possuem vínculo com nenhum Cliente.
     *
     * O frontend usa este endpoint ao inicializar (ou após reconexão
     * WebSocket) para exibir o painel de triagem de contatos pendentes,
     * complementando os alertas em tempo real do tópico
     * /topic/contatos-pendentes.
     *
     * Ordenação: mais recentes primeiro (já definida no repository).
     */
    @GetMapping("/pendentes")
    public ResponseEntity<Page<ContatoPendenteResponse>> pendentes(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listarPendentes(pageable));
    }

    @PostMapping
    public ResponseEntity<ContatoResponse> criar(@Valid @RequestBody ContatoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContatoResponse> atualizar(@PathVariable UUID id,
            @Valid @RequestBody ContatoRequest req) {
        return ResponseEntity.ok(service.atualizar(id, req));
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}