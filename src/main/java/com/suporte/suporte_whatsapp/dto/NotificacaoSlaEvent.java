package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.NivelSla;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class NotificacaoSlaEvent {
    private UUID chamadoId;
    private NivelSla nivel;
    private String mensagem;
    private LocalDateTime timestamp;
}