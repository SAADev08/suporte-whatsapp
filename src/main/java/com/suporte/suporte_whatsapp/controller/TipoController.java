package com.suporte.suporte_whatsapp.controller;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.service.TipoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/tipos")
@RequiredArgsConstructor
public class TipoController {

    private final TipoService service;

    @GetMapping
    public ResponseEntity<List<TipoResponse>> listar() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @PostMapping
    public ResponseEntity<TipoResponse> criar(@Valid @RequestBody TipoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(req));
    }

    @GetMapping("/{id}/subtipos")
    public ResponseEntity<List<SubtipoResponse>> listarSubtipos(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listarSubtipos(id));
    }

    @PostMapping("/{id}/subtipos")
    public ResponseEntity<SubtipoResponse> criarSubtipo(@PathVariable UUID id,
            @Valid @RequestBody SubtipoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarSubtipo(id, req));
    }
}
