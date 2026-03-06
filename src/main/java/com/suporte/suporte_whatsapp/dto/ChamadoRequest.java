package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.*;

@Data
public class ChamadoRequest {
    private String texto;
    private Categoria categoria;
    private String solucao;
    private ChamadoStatus statusAtual;
    @NotNull
    private Origem origem;
    @NotNull
    private UUID contatoId;
    private UUID usuarioResponsavelId;
    private UUID subtipoId;
    private List<UUID> chatIds;
}
