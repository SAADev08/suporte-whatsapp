package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.ChamadoStatusHistorico;
import com.suporte.suporte_whatsapp.model.enums.ChamadoStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ChamadoStatusHistoricoResponse {
    private UUID id;
    private ChamadoStatus status;
    private LocalDateTime dtInicio;
    private LocalDateTime dtFim;
    private Long tempoEmStatusSegundos;
    private UUID usuarioResponsavelId;
    private String usuarioResponsavelNome;

    public static ChamadoStatusHistoricoResponse from(ChamadoStatusHistorico h) {
        var r = new ChamadoStatusHistoricoResponse();
        r.id = h.getId();
        r.status = h.getStatus();
        r.dtInicio = h.getDtInicio();
        r.dtFim = h.getDtFim();
        r.tempoEmStatusSegundos = h.getTempoEmStatusSegundos();
        if (h.getUsuarioResponsavel() != null) {
            r.usuarioResponsavelId = h.getUsuarioResponsavel().getId();
            r.usuarioResponsavelNome = h.getUsuarioResponsavel().getNome();
        }
        return r;
    }
}
