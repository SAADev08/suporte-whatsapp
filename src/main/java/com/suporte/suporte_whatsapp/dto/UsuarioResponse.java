package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.Usuario;
import com.suporte.suporte_whatsapp.model.enums.Perfil;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UsuarioResponse {
    private UUID id;
    private String nome;
    private String email;
    private Perfil perfil;
    private Boolean ativo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UsuarioResponse from(Usuario u) {
        var r = new UsuarioResponse();
        r.id = u.getId();
        r.nome = u.getNome();
        r.email = u.getEmail();
        r.perfil = u.getPerfil();
        r.ativo = u.getAtivo();
        r.createdAt = u.getCreatedAt();
        r.updatedAt = u.getUpdatedAt();
        return r;
    }
}