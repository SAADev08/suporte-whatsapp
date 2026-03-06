package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.TipoMidia;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WebhookWhatsappRequest {
    private String foneCliente;
    private String nomeContato;
    private String nomeGrupo;
    private String texto;
    private String fileUrl;
    private TipoMidia tipoMidia = TipoMidia.TEXTO;
    private LocalDateTime dtEnvio;
}
