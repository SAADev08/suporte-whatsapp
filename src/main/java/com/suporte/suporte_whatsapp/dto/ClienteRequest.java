package com.suporte.suporte_whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequest {
    @NotBlank
    private String nome;
    private String cidade;
    @NotBlank
    private String cpfCnpj;
    private String contatoPrincipal;
    private String comercialResponsavel;
}