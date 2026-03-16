package com.suporte.suporte_whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO que espelha fielmente o payload enviado pela Z-API no webhook
 * "Upon receiving" (on-message-received).
 *
 * A Z-API envia um único envelope JSON para todos os tipos de mensagem.
 * O tipo é identificado pela presença dos objetos aninhados:
 * - text → mensagem de texto
 * - image → imagem
 * - audio → áudio / mensagem de voz
 * - video → vídeo
 * - document → documento/arquivo
 *
 * Campos de referência:
 * https://developer.z-api.io/en/webhooks/on-message-received
 */
@Data
public class WebhookWhatsappRequest {

    // -------------------------------------------------------------------------
    // Campos do envelope (nível raiz)
    // -------------------------------------------------------------------------

    /** ID único da mensagem na conversa — usado para idempotência (Fase 4). */
    private String messageId;

    /**
     * Número do remetente (contato individual) OU ID do grupo.
     * Para mensagens de grupo, é o ID do grupo (ex:
     * "5544999999999-1234567890@g.us").
     * Para mensagens individuais, é o telefone no formato internacional:
     * "5544999999999".
     */
    private String phone;

    /** Número do WhatsApp conectado à instância Z-API. */
    private String connectedPhone;

    /** Nome exibido do remetente na conversa. */
    private String senderName;

    /** URL da foto de perfil do remetente. */
    private String senderPhoto;

    /**
     * true se a mensagem veio de um grupo.
     * Campo renomeado de "isGroup" para "group" para evitar que o Lombok
     * gere isIsGroup() — o @JsonProperty garante o mapeamento correto com
     * o payload da Z-API que usa a chave "isGroup".
     */
    @JsonProperty("isGroup")
    private boolean group;

    /**
     * Telefone do membro do grupo que enviou a mensagem.
     * Preenchido apenas quando isGroup=true; null em conversas individuais.
     */
    private String participantPhone;

    /** Nome do grupo. Preenchido apenas quando isGroup=true. */
    private String chatName;

    /**
     * Momento do envio em epoch milliseconds.
     * A Z-API usa o campo "momment" (typo intencional na API deles).
     */
    private Long momment;

    /**
     * true se a mensagem foi enviada pela própria instância.
     * Mesmo tratamento: renomeado de "fromMe" para "sentByMe" para evitar
     * ambiguidade, com @JsonProperty mapeando o campo correto do JSON.
     */
    @JsonProperty("fromMe")
    private boolean sentByMe;

    /** true se a mensagem está aguardando entrega (offline/fila). */
    private boolean waitingMessage;

    /** Tipo do evento — esperado "ReceivedCallback" para mensagens recebidas. */
    private String type;

    // -------------------------------------------------------------------------
    // Objetos aninhados por tipo de mídia
    // Exatamente um deles estará presente por mensagem; os demais serão null.
    // -------------------------------------------------------------------------

    /** Presente quando a mensagem é de texto. */
    private TextPayload text;

    /** Presente quando a mensagem contém uma imagem. */
    private ImagePayload image;

    /** Presente quando a mensagem contém áudio ou mensagem de voz. */
    private AudioPayload audio;

    /** Presente quando a mensagem contém vídeo. */
    private VideoPayload video;

    /** Presente quando a mensagem contém documento/arquivo. */
    private DocumentPayload document;

    // =========================================================================
    // Objetos aninhados (inner classes)
    // =========================================================================

    @Data
    public static class TextPayload {
        /** Conteúdo textual da mensagem. */
        private String message;
    }

    @Data
    public static class ImagePayload {
        /** Legenda da imagem (pode ser null). */
        private String caption;
        /** URL da imagem com prazo de expiração de 30 dias (Z-API storage). */
        private String imageUrl;
        /** URL da miniatura da imagem. */
        private String thumbnailUrl;
        /** MIME type da imagem (ex: "image/jpeg"). */
        private String mimeType;
    }

    @Data
    public static class AudioPayload {
        /** URL do arquivo de áudio com prazo de expiração de 30 dias. */
        private String audioUrl;
        /** MIME type do áudio (ex: "audio/ogg; codecs=opus"). */
        private String mimeType;
    }

    @Data
    public static class VideoPayload {
        /** Legenda do vídeo (pode ser null). */
        private String caption;
        /** URL do arquivo de vídeo com prazo de expiração de 30 dias. */
        private String videoUrl;
        /** MIME type do vídeo (ex: "video/mp4"). */
        private String mimeType;
    }

    @Data
    public static class DocumentPayload {
        /** Nome do arquivo conforme enviado. */
        private String fileName;
        /** Título do documento (pode ser null). */
        private String title;
        /** URL de download do documento com prazo de expiração de 30 dias. */
        private String documentUrl;
        /** URL da miniatura do documento. */
        private String thumbnailUrl;
        /** MIME type do documento (ex: "application/pdf"). */
        private String mimeType;
    }
}