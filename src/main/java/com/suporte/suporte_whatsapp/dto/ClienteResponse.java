package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.Cliente;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ClienteResponse {
    private UUID id;
    private String nome;
    private String cidade;
    private String cpfCnpj;
    private String contatoPrincipal;
    private String comercialResponsavel;
    private Boolean ativo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ClienteResponse from(Cliente c) {
        var r = new ClienteResponse();
        r.id = c.getId();
        r.nome = c.getNome();
        r.cidade = c.getCidade();
        r.cpfCnpj = c.getCpfCnpj();
        r.contatoPrincipal = c.getContatoPrincipal();
        r.comercialResponsavel = c.getComercialResponsavel();
        r.ativo = c.getAtivo();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        return r;
    }
}
