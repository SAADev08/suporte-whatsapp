package com.suporte.suporte_whatsapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.*;

@Data
public class ContatoRequest {
    @NotBlank
    private String nome;
    @NotBlank
    private String telefone;
    private String email;
    @NotEmpty(message = "Contato deve estar vinculado a pelo menos um cliente")
    private List<UUID> clienteIds;
}