package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.Chat;
import com.suporte.suporte_whatsapp.model.enums.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ChatResponse {
    private UUID id;
    private UUID chamadoId;
    private UUID contatoId;
    private String nomeContato;
    private UUID usuarioId;
    private ChatOrigem origem;
    private LocalDateTime dtEnvio;
    private String texto;
    private String fileUrl;
    private TipoMidia tipoMidia;
    private String foneCliente;
    private String foneSuporte;
    private String nomeGrupo;
    private LocalDateTime createdAt;

    public static ChatResponse from(Chat c) {
        var r = new ChatResponse();
        r.id = c.getId();
        r.chamadoId = c.getChamado() != null ? c.getChamado().getId() : null;
        r.contatoId = c.getContato().getId();
        r.nomeContato = c.getNomeContato();
        r.usuarioId = c.getUsuario() != null ? c.getUsuario().getId() : null;
        r.origem = c.getOrigem();
        r.dtEnvio = c.getDtEnvio();
        r.texto = c.getTexto();
        r.fileUrl = c.getFileUrl();
        r.tipoMidia = c.getTipoMidia();
        r.foneCliente = c.getFoneCliente();
        r.foneSuporte = c.getFoneSuporte();
        r.nomeGrupo = c.getNomeGrupo();
        r.createdAt = c.getCreatedAt();
        return r;
    }
}
