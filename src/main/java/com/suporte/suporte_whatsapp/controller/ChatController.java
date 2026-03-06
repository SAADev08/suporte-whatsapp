package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.Usuario;
import com.suporte.suporte_whatsapp.repository.UsuarioRepository;
import com.suporte.suporte_whatsapp.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService service;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/fila")
    public ResponseEntity<List<ChatResponse>> fila() {
        return ResponseEntity.ok(service.fila());
    }

    @GetMapping("/chamado/{chamadoId}")
    public ResponseEntity<List<ChatResponse>> porChamado(@PathVariable UUID chamadoId) {
        return ResponseEntity.ok(service.porChamado(chamadoId));
    }

    @PostMapping("/responder")
    public ResponseEntity<ChatResponse> responder(@Valid @RequestBody ChatRequest req,
            Authentication auth) {
        Usuario analista = usuarioRepository.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(service.responder(req, analista));
    }
}