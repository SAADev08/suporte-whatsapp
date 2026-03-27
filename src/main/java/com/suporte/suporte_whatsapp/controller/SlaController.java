package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.NotificacaoSlaEvent;
import com.suporte.suporte_whatsapp.service.SlaNotificacaoCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expõe o snapshot dos alertas SLA ativos via REST.
 *
 * O frontend consulta GET /api/sla/ativos ao reconectar o WebSocket
 * (ou ao recarregar a página) para recuperar alertas que foram emitidos
 * enquanto o WebSocket estava desconectado — evitando a janela cega entre
 * o restart do backend e a reconexão do cliente.
 */
@RestController
@RequestMapping("/api/sla")
@RequiredArgsConstructor
public class SlaController {

    private final SlaNotificacaoCache slaCache;

    /**
     * Retorna todos os alertas SLA ativos no momento.
     * Cada item tem o mesmo formato do {@link NotificacaoSlaEvent} emitido
     * via WebSocket, permitindo que o frontend use o mesmo handler.
     */
    @GetMapping("/ativos")
    public ResponseEntity<List<NotificacaoSlaEvent>> ativos() {
        List<NotificacaoSlaEvent> eventos = slaCache.listarAtivos().stream()
                .map(a -> new NotificacaoSlaEvent(
                        a.chamadoId(),
                        a.nivel(),
                        a.mensagem(),
                        a.timestamp()))
                .toList();

        return ResponseEntity.ok(eventos);
    }
}