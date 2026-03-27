package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.NotificacaoTriagemEvent;
import com.suporte.suporte_whatsapp.service.TriagemNotificacaoCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expõe o snapshot dos alertas SLA de triagem ativos via REST.
 *
 * O frontend consulta GET /api/sla/triagem/ativos ao reconectar o WebSocket
 * para recuperar alertas emitidos enquanto estava desconectado.
 */
@RestController
@RequestMapping("/api/sla/triagem")
@RequiredArgsConstructor
public class SlaTriagemController {

    private final TriagemNotificacaoCache triagemCache;

    /**
     * Retorna todos os alertas de triagem ativos no momento.
     * Mesmo formato do {@link NotificacaoTriagemEvent} emitido via WebSocket.
     */
    @GetMapping("/ativos")
    public ResponseEntity<List<NotificacaoTriagemEvent>> ativos() {
        List<NotificacaoTriagemEvent> eventos = triagemCache.listarAtivos().stream()
                .map(a -> new NotificacaoTriagemEvent(
                        a.contatoId(),
                        a.nomeContato(),
                        a.nivel(),
                        a.mensagem(),
                        a.timestamp()))
                .toList();

        return ResponseEntity.ok(eventos);
    }
}
