package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.Contato;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para contatos pendentes de vinculação com Cliente.
 *
 * Retornado pelo endpoint GET /api/contatos/pendentes e também
 * emitido via WebSocket no tópico /topic/contatos-pendentes sempre
 * que um novo contato desconhecido chega via webhook da Z-API.
 *
 * Contém apenas os campos necessários para o analista identificar
 * o contato e tomar a ação de vinculação — sem expor dados
 * desnecessários da entidade completa.
 */
@Data
public class ContatoPendenteResponse {

    private UUID id;
    private String nome;
    private String telefone;
    private LocalDateTime createdAt;

    public static ContatoPendenteResponse from(Contato c) {
        var r = new ContatoPendenteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.telefone = c.getTelefone();
        r.createdAt = c.getCreatedAt();
        return r;
    }
}