package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.Contato;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class ContatoResponse {
    private UUID id;
    private String nome;
    private String telefone;
    private String email;
    private Boolean ativo;
    private Boolean pendenteVinculacao;
    private List<ClienteResponse> clientes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContatoResponse from(Contato c) {
        var r = new ContatoResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.telefone = c.getTelefone();
        r.email = c.getEmail();
        r.ativo = c.getAtivo();
        r.pendenteVinculacao = c.getPendenteVinculacao();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        r.clientes = c.getClientes().stream().map(ClienteResponse::from).toList();
        return r;
    }
}