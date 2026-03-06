package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.Perfil;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UsuarioRequest {
    @NotBlank
    private String nome;
    @NotBlank
    @Email
    private String email;
    private String senha;
    @NotNull
    private Perfil perfil;
}
