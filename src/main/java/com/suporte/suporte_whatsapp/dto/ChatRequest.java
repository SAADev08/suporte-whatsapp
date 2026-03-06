package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.TipoMidia;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ChatRequest {
    @NotNull
    private UUID chamadoId;
    private String texto;
    private String fileUrl;
    private TipoMidia tipoMidia = TipoMidia.TEXTO;
}
