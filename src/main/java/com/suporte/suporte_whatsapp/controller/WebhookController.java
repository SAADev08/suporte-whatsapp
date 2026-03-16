package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.WebhookWhatsappRequest;
import com.suporte.suporte_whatsapp.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService service;

    @PostMapping("/whatsapp")
    public ResponseEntity<Void> receber(@RequestBody WebhookWhatsappRequest req) {
        service.processar(req);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handlePayloadInvalido(HttpMessageNotReadableException ex) {
        log.warn("[Webhook] Payload JSON inválido recebido — descartando. Causa: {}",
                ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "status", "erro",
                        "motivo", "Payload inválido ou malformado",
                        "detalhe", ex.getMostSpecificCause().getMessage()));
    }
}
