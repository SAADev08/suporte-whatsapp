package com.suporte.suporte_whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TipoRequest {
    @NotBlank
    private String nome;
}
