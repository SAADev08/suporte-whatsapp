package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.Tipo;
import lombok.Data;
import java.util.UUID;

@Data
public class TipoResponse {
    private UUID id;
    private String nome;
    private Boolean ativo;

    public static TipoResponse from(Tipo t) {
        var r = new TipoResponse();
        r.id = t.getId();
        r.nome = t.getNome();
        r.ativo = t.getAtivo();
        return r;
    }
}