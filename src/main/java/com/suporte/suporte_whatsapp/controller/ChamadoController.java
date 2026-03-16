package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.enums.*;
import com.suporte.suporte_whatsapp.service.ChamadoService;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService service;

    @GetMapping
    public ResponseEntity<Page<ChamadoResponse>> listar(
            @RequestParam(required = false) ChamadoStatus status,
            @RequestParam(required = false) Origem origem,
            @RequestParam(required = false) UUID contatoId,
            @RequestParam(required = false) UUID usuarioId,
            @PageableDefault(size = 20, sort = "dtAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listar(status, origem, contatoId, usuarioId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChamadoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> criar(@Valid @RequestBody ChamadoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChamadoResponse> atualizar(@PathVariable UUID id,
            @RequestBody ChamadoRequest req) {
        return ResponseEntity.ok(service.atualizar(id, req));
    }

    @PatchMapping("/{id}/encerrar")
    public ResponseEntity<ChamadoResponse> encerrar(@PathVariable UUID id,
            @RequestBody EncerrarRequest req) {
        return ResponseEntity.ok(service.encerrar(id, req.getSolucao()));
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<ChamadoStatusHistoricoResponse>> historico(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarHistorico(id));
    }

    @Data
    public static class EncerrarRequest {
        private String solucao;
    }
}