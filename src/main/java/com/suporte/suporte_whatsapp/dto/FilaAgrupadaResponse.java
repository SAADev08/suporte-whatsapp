package com.suporte.suporte_whatsapp.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa um grupo de mensagens pendentes (sem chamado) de um mesmo
 * contato, retornado pelo endpoint GET /api/chat/fila/agrupada.
 *
 * Cada item desta lista representa um contato que tem pelo menos uma
 * mensagem aguardando triagem — ou seja, mensagens em CHAT sem id_chamado.
 *
 * Campos pensados para a tela de triagem do analista:
 *
 * - contatoId / nomeContato / foneContato → identificar de quem é
 * - totalMensagens → quantas mensagens estão aguardando
 * - dtPrimeiraMensagem → há quanto tempo este contato está esperando (SLA)
 * - dtUltimaMensagem → quando chegou a mensagem mais recente
 * - ultimoTexto → preview da última mensagem (texto ou descrição de mídia)
 * - pendenteVinculacao → se TRUE, o analista precisa vincular o contato a
 * um Cliente antes de poder criar o chamado
 */
@Data
public class FilaAgrupadaResponse {

    private UUID contatoId;
    private String nomeContato;
    private String foneContato;
    private boolean pendenteVinculacao;
    private long totalMensagens;
    private LocalDateTime dtPrimeiraMensagem;
    private LocalDateTime dtUltimaMensagem;

    /**
     * Preview da última mensagem para exibição na lista.
     * Para mídias não-texto, o frontend pode usar tipoMidia para exibir
     * um ícone adequado (🎵 Áudio, 📷 Imagem, etc.).
     */
    private String ultimoTexto;
    private String ultimoTipoMidia;

    /**
     * Construtor usado pela query JPQL via projeção de construtor.
     * A ordem dos parâmetros deve ser idêntica à ordem das expressões
     * no SELECT da query em ChatRepository.
     */
    public FilaAgrupadaResponse(
            UUID contatoId,
            String nomeContato,
            String foneContato,
            boolean pendenteVinculacao,
            long totalMensagens,
            LocalDateTime dtPrimeiraMensagem,
            LocalDateTime dtUltimaMensagem) {
        this.contatoId = contatoId;
        this.nomeContato = nomeContato;
        this.foneContato = foneContato;
        this.pendenteVinculacao = pendenteVinculacao;
        this.totalMensagens = totalMensagens;
        this.dtPrimeiraMensagem = dtPrimeiraMensagem;
        this.dtUltimaMensagem = dtUltimaMensagem;
    }
}