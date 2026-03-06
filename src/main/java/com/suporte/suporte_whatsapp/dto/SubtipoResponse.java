package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.Subtipo;
import lombok.Data;
import java.util.UUID;

@Data
public class SubtipoResponse {
    private UUID id;
    private String nome;
    private Boolean ativo;
    private UUID tipoId;
    private String tipoNome;

    public static SubtipoResponse from(Subtipo s) {
        var r = new SubtipoResponse();
        r.id = s.getId();
        r.nome = s.getNome();
        r.ativo = s.getAtivo();
        r.tipoId = s.getTipo().getId();
        r.tipoNome = s.getTipo().getNome();
        return r;
    }
}