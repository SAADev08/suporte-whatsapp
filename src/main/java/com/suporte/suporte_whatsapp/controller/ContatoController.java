package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.service.ContatoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/contatos")
@RequiredArgsConstructor
public class ContatoController {

    private final ContatoService service;

    @GetMapping
    public ResponseEntity<List<ContatoResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(service.listar(nome, telefone, email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContatoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
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