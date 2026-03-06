package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.*;
import com.suporte.suporte_whatsapp.model.enums.*;
import com.suporte.suporte_whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ContatoRepository contatoRepository;
    private final ChatRepository chatRepository;
    private final ChamadoRepository chamadoRepository;
    private final SimpMessagingTemplate ws;

    @Transactional
    public void processar(WebhookWhatsappRequest req) {
        Contato contato = contatoRepository.findByTelefone(req.getFoneCliente())
                .orElseGet(() -> contatoRepository.save(
                        Contato.builder()
                                .nome(req.getNomeContato() != null
                                        ? req.getNomeContato()
                                        : req.getFoneCliente())
                                .telefone(req.getFoneCliente())
                                .build()));

        List<Chamado> abertos = chamadoRepository.findAbertosParaContato(contato.getId());
        Chamado chamadoVinculado = abertos.isEmpty() ? null : abertos.get(0);

        Chat msg = Chat.builder()
                .chamado(chamadoVinculado).contato(contato)
                .origem(ChatOrigem.CLIENTE)
                .dtEnvio(req.getDtEnvio() != null ? req.getDtEnvio() : LocalDateTime.now())
                .texto(req.getTexto()).fileUrl(req.getFileUrl())
                .tipoMidia(req.getTipoMidia() != null ? req.getTipoMidia() : TipoMidia.TEXTO)
                .foneCliente(req.getFoneCliente())
                .nomeGrupo(req.getNomeGrupo())
                .nomeContato(contato.getNome())
                .build();
        msg = chatRepository.save(msg);

        ws.convertAndSend("/topic/mensagens", ChatResponse.from(msg));
    }
}
