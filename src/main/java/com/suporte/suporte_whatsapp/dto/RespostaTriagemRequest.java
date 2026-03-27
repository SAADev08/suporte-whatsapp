package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.enums.TipoMidia;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

/**
 * Payload para o analista responder a um contato diretamente na triagem,
 * sem a necessidade de abrir um chamado formal.
 *
 * A mensagem é salva em CHAT com id_chamado = NULL e origem = SUPORTE,
 * e enviada via Z-API normalmente.
 *
 * POST /api/chat/responder-triagem
 */
@Data
public class RespostaTriagemRequest {

    /**
     * ID do contato destinatário.
     * O telefone é obtido do cadastro do contato no banco.
     */
    @NotNull(message = "contatoId é obrigatório")
    private UUID contatoId;

    private String texto;
    private String fileUrl;
    private TipoMidia tipoMidia = TipoMidia.TEXTO;
}