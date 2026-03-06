package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.WebhookWhatsappRequest;
import com.suporte.suporte_whatsapp.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
