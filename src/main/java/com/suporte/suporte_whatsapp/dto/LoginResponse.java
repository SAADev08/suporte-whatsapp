package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.Perfil;
import lombok.*;
import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UsuarioInfo usuario;

    @Data
    @AllArgsConstructor
    public static class UsuarioInfo {
        private UUID id;
        private String nome;
        private Perfil perfil;
    }
}