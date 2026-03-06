package com.suporte.suporte_whatsapp.dto;

import com.suporte.suporte_whatsapp.model.Chamado;
import com.suporte.suporte_whatsapp.model.enums.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class ChamadoResponse {
    private UUID id;
    private String texto;
    private Categoria categoria;
    private String solucao;
    private ChamadoStatus statusAtual;
    private Origem origem;
    private LocalDateTime dtAbertura;
    private LocalDateTime dtEncerramento;
    private LocalDateTime dtPrimeiraMensagem;
    private LocalDateTime dtPrimeiraResposta;
    private Long tempoTotalSegundos;
    private UUID contatoId;
    private String contatoNome;
    private UUID usuarioResponsavelId;
    private String usuarioResponsavelNome;
    private UUID subtipoId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChamadoStatusHistoricoResponse> historico;

    public static ChamadoResponse from(Chamado c) {
        var r = new ChamadoResponse();
        r.id = c.getId();
        r.texto = c.getTexto();
        r.categoria = c.getCategoria();
        r.solucao = c.getSolucao();
        r.statusAtual = c.getStatusAtual();
        r.origem = c.getOrigem();
        r.dtAbertura = c.getDtAbertura();
        r.dtEncerramento = c.getDtEncerramento();
        r.dtPrimeiraMensagem = c.getDtPrimeiraMensagem();
        r.dtPrimeiraResposta = c.getDtPrimeiraResposta();
        r.tempoTotalSegundos = c.getTempoTotalSegundos();
        r.contatoId = c.getContato().getId();
        r.contatoNome = c.getContato().getNome();
        if (c.getUsuarioResponsavel() != null) {
            r.usuarioResponsavelId = c.getUsuarioResponsavel().getId();
            r.usuarioResponsavelNome = c.getUsuarioResponsavel().getNome();
        }
        if (c.getSubtipo() != null)
            r.subtipoId = c.getSubtipo().getId();
        r.createdAt = c.getCreatedAt();
        r.updatedAt = c.getUpdatedAt();
        return r;
    }
}
