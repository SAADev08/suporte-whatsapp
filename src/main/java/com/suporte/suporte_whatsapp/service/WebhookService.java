package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.ChatResponse;
import com.suporte.suporte_whatsapp.dto.ContatoPendenteResponse;
import com.suporte.suporte_whatsapp.dto.WebhookWhatsappRequest;
import com.suporte.suporte_whatsapp.dto.WsEnvelope;
import com.suporte.suporte_whatsapp.model.*;
import com.suporte.suporte_whatsapp.model.enums.*;
import com.suporte.suporte_whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

        private final ContatoRepository contatoRepository;
        private final ChatRepository chatRepository;
        private final ChamadoRepository chamadoRepository;
        private final SimpMessagingTemplate ws;

        // =========================================================================
        // Ponto de entrada
        // =========================================================================

        /**
         * Processa um webhook recebido da Z-API.
         *
         * ─── Estratégia de tratamento de erros ───────────────────────────────────
         *
         * Este método NUNCA lança exceção para o controller. A razão é protocolar:
         * qualquer não-2xx retornado à Z-API faz ela retentar o mesmo webhook de
         * forma exponencial e potencialmente infinita, resultando em mensagens
         * duplicadas no banco de dados.
         *
         * Hierarquia de tratamento:
         *
         * 1. Validação de pré-condições (payload nulo, phone ausente)
         * → lança IllegalArgumentException capturada aqui → log WARN → retorno limpo
         * → HTTP 200 ao controller (Z-API não retenta, payload foi intencionalmente
         * inválido)
         *
         * 2. Erros de infraestrutura (banco fora, timeout, NPE inesperado)
         * → capturados pelo catch(Exception) → log ERROR com contexto completo
         * → HTTP 200 ao controller (evita retentativa infinita da Z-API)
         * → o log contém messageId, phone e stack trace para triagem manual
         *
         * Sobre auditoria em banco: propositalmente NÃO foi implementada tabela
         * de webhook_falha. Se o banco está fora, a própria auditoria falharia em
         * cascata. Os logs estruturados com messageId são suficientes para
         * reprocessamento manual e não criam dependência circular com a infra.
         */
        public void processar(WebhookWhatsappRequest req) {
                try {
                        processarInterno(req);
                } catch (IllegalArgumentException e) {
                        // Erro de validação — payload semanticamente inválido.
                        // Logado como WARN: não é um bug, é dado ruim vindo de fora.
                        log.warn("[Webhook] Payload descartado por validação. messageId={} | motivo={}",
                                        extrairMessageId(req), e.getMessage());
                } catch (Exception e) {
                        // Erro inesperado de infraestrutura ou lógica.
                        // Logado como ERROR com contexto completo para triagem manual.
                        log.error("[Webhook] Falha ao processar mensagem. messageId={} | phone=...{} | erro={}",
                                        extrairMessageId(req),
                                        extrairUltimos4(req),
                                        e.getMessage(),
                                        e);
                }
        }

        // =========================================================================
        // Lógica principal — separada para manter o try/catch limpo
        // =========================================================================

        @Transactional
        private void processarInterno(WebhookWhatsappRequest req) {

                // 1. Validar pré-condições mínimas do payload
                validar(req);

                // 2. Verificar idempotência — descartar se messageId já foi processado
                if (req.getMessageId() != null && chatRepository.existsByMessageId(req.getMessageId())) {
                        log.debug("[Webhook] Mensagem duplicada descartada — messageId={} já existe.",
                                        req.getMessageId());
                        return;
                }

                // 3. Ignorar mensagens enviadas pela própria instância (eco de saída)
                if (req.isSentByMe()) {
                        log.debug("[Webhook] Mensagem ignorada — fromMe=true, messageId={}", req.getMessageId());
                        return;
                }

                // 4. Normalizar para modelo interno agnóstico de tipo de mídia
                MensagemNormalizada msg = normalizar(req);
                if (msg == null) {
                        log.debug("[Webhook] Tipo de mensagem não suportado ignorado. messageId={}",
                                        req.getMessageId());
                        return;
                }

                // 5. Resolver ou criar o Contato pelo telefone do remetente
                boolean[] contatoNovo = { false };
                Contato contato = resolverContato(msg.foneRemetente(), msg.nomeRemetente(), contatoNovo);

                // 6. Tentar vincular a um chamado aberto existente para este contato
                List<Chamado> abertos = chamadoRepository.findAbertosParaContato(contato.getId());
                Chamado chamadoVinculado = resolverChamadoParaVinculo(abertos, contato.getId());

                // 7. Persistir a mensagem no CHAT (independente do status do contato)
                Chat chat = Chat.builder()
                                .messageId(req.getMessageId())
                                .chamado(chamadoVinculado)
                                .contato(contato)
                                .origem(ChatOrigem.CLIENTE)
                                .dtEnvio(msg.dtEnvio())
                                .texto(msg.texto())
                                .fileUrl(msg.fileUrl())
                                .tipoMidia(msg.tipoMidia())
                                .foneCliente(msg.foneRemetente())
                                .nomeGrupo(msg.nomeGrupo())
                                .nomeContato(contato.getNome())
                                .build();

                chat = chatRepository.save(chat);

                log.info("[Webhook] Mensagem {} recebida de número terminado em {} — tipo={}, chamado={}",
                                req.getMessageId(),
                                ultimos4Digitos(msg.foneRemetente()),
                                msg.tipoMidia(),
                                chamadoVinculado != null ? chamadoVinculado.getId() : "sem chamado");

                // 8. Notificar frontends sobre a nova mensagem
                ChatResponse chatResponse = ChatResponse.from(chat);
                ws.convertAndSend("/topic/mensagens", WsEnvelope.of(chatResponse));
                ws.convertAndSend("/topic/contato/" + contato.getId(), WsEnvelope.of(chatResponse));

                // 9. Se o contato acabou de ser criado via webhook, alertar analistas
                if (contatoNovo[0]) {
                        ContatoPendenteResponse alerta = ContatoPendenteResponse.from(contato);
                        ws.convertAndSend("/topic/contatos-pendentes", WsEnvelope.of(alerta));
                        log.info("[Webhook] Alerta de contato pendente emitido para número terminado em {}",
                                        ultimos4Digitos(msg.foneRemetente()));
                }
        }

        // =========================================================================
        // Validação de pré-condições
        // =========================================================================

        /**
         * Valida os campos mínimos necessários para processar o webhook.
         * Lança IllegalArgumentException para payloads semanticamente inválidos —
         * capturada em processar() e logada como WARN (não é bug, é dado ruim).
         */
        private void validar(WebhookWhatsappRequest req) {
                if (req == null) {
                        throw new IllegalArgumentException("Request nulo");
                }
                if (req.getPhone() == null || req.getPhone().isBlank()) {
                        throw new IllegalArgumentException("Campo 'phone' ausente ou vazio");
                }
                if (req.getMomment() == null) {
                        throw new IllegalArgumentException("Campo 'momment' ausente");
                }
        }

        // =========================================================================
        // Normalização: Z-API payload → modelo interno
        // =========================================================================

        private MensagemNormalizada normalizar(WebhookWhatsappRequest req) {
                String foneRemetente = (req.isGroup() && req.getParticipantPhone() != null)
                                ? req.getParticipantPhone()
                                : req.getPhone();

                String nomeRemetente = req.getSenderName() != null ? req.getSenderName() : foneRemetente;
                String nomeGrupo = req.isGroup() ? req.getChatName() : null;
                LocalDateTime dtEnvio = converterMomment(req.getMomment());

                if (req.getText() != null && req.getText().getMessage() != null) {
                        return new MensagemNormalizada(foneRemetente, nomeRemetente, nomeGrupo, dtEnvio,
                                        req.getText().getMessage(), null, TipoMidia.TEXTO);
                }
                if (req.getImage() != null && req.getImage().getImageUrl() != null) {
                        return new MensagemNormalizada(foneRemetente, nomeRemetente, nomeGrupo, dtEnvio,
                                        req.getImage().getCaption(), req.getImage().getImageUrl(), TipoMidia.IMAGEM);
                }
                if (req.getAudio() != null && req.getAudio().getAudioUrl() != null) {
                        return new MensagemNormalizada(foneRemetente, nomeRemetente, nomeGrupo, dtEnvio,
                                        null, req.getAudio().getAudioUrl(), TipoMidia.AUDIO);
                }
                if (req.getVideo() != null && req.getVideo().getVideoUrl() != null) {
                        return new MensagemNormalizada(foneRemetente, nomeRemetente, nomeGrupo, dtEnvio,
                                        req.getVideo().getCaption(), req.getVideo().getVideoUrl(), TipoMidia.VIDEO);
                }
                if (req.getDocument() != null && req.getDocument().getDocumentUrl() != null) {
                        return new MensagemNormalizada(foneRemetente, nomeRemetente, nomeGrupo, dtEnvio,
                                        req.getDocument().getFileName(), req.getDocument().getDocumentUrl(),
                                        TipoMidia.VIDEO);
                }
                return null;
        }

        // =========================================================================
        // Helpers
        // =========================================================================

        private Contato resolverContato(String telefone, String nome, boolean[] foiCriado) {
                return contatoRepository.findByTelefone(telefone)
                                .orElseGet(() -> {
                                        foiCriado[0] = true;
                                        log.info("[Webhook] Novo contato detectado — número terminado em {}. " +
                                                        "Criando com pendenteVinculacao=true.",
                                                        ultimos4Digitos(telefone));
                                        return contatoRepository.save(
                                                        Contato.builder()
                                                                        .nome(nome != null ? nome : telefone)
                                                                        .telefone(telefone)
                                                                        .pendenteVinculacao(true)
                                                                        .build());
                                });
        }

        private Chamado resolverChamadoParaVinculo(List<Chamado> abertos, UUID contatoId) {
                if (abertos.isEmpty()) {
                        return null;
                }
                if (abertos.size() == 1) {
                        return abertos.get(0);
                }
                // Verifica empate no status de maior prioridade (primeiro da lista)
                ChamadoStatus statusPrioritario = abertos.get(0).getStatusAtual();
                long countMesmoPrioridade = abertos.stream()
                                .filter(ch -> ch.getStatusAtual() == statusPrioritario)
                                .count();

                if (countMesmoPrioridade > 1) {
                        log.warn("[Webhook] Ambiguidade: contato {} possui {} chamados abertos com status {}. " +
                                        "Mensagem enviada para triagem manual.",
                                        contatoId, countMesmoPrioridade, statusPrioritario);
                        return null;
                }
                return abertos.get(0);
        }

        private LocalDateTime converterMomment(Long momment) {
                if (momment == null)
                        return LocalDateTime.now();
                return LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(momment),
                                ZoneId.of("America/Cuiaba"));
        }

        /**
         * Extrai os últimos 4 dígitos do telefone para log — nunca loga o número
         * completo (LGPD).
         */
        private String ultimos4Digitos(String telefone) {
                if (telefone == null || telefone.length() < 4)
                        return "????";
                return "..." + telefone.substring(telefone.length() - 4);
        }

        /**
         * Extrai messageId de forma segura para uso nos logs de erro (req pode ser
         * nulo).
         */
        private String extrairMessageId(WebhookWhatsappRequest req) {
                if (req == null || req.getMessageId() == null)
                        return "desconhecido";
                return req.getMessageId();
        }

        /**
         * Extrai últimos 4 dígitos do phone de forma segura para uso nos logs de erro.
         */
        private String extrairUltimos4(WebhookWhatsappRequest req) {
                if (req == null || req.getPhone() == null)
                        return "????";
                return ultimos4Digitos(req.getPhone());
        }

        // =========================================================================
        // Estrutura interna de normalização
        // =========================================================================

        private record MensagemNormalizada(
                        String foneRemetente,
                        String nomeRemetente,
                        String nomeGrupo,
                        LocalDateTime dtEnvio,
                        String texto,
                        String fileUrl,
                        TipoMidia tipoMidia) {
        }
}