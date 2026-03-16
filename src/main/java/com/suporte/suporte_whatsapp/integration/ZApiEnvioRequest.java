package com.suporte.suporte_whatsapp.integration;

import lombok.Builder;
import lombok.Getter;

/**
 * DTOs de payload para os endpoints de envio da Z-API.
 *
 * A Z-API possui endpoints distintos por tipo de mídia, cada um com
 * seu próprio corpo JSON. Esta classe agrupa os records imutáveis
 * correspondentes a cada tipo, evitando proliferação de arquivos.
 *
 * Referência: https://developer.z-api.io/messages/send-text
 */
public final class ZApiEnvioRequest {

    private ZApiEnvioRequest() {}

    // -------------------------------------------------------------------------
    // POST /send-text
    // -------------------------------------------------------------------------

    /**
     * Payload para envio de mensagem de texto simples.
     *
     * @param phone   Número no formato internacional sem "+": "5565999990000"
     * @param message Texto da mensagem
     */
    @Getter
    @Builder
    public static class Texto {
        private final String phone;
        private final String message;
    }

    // -------------------------------------------------------------------------
    // POST /send-image
    // -------------------------------------------------------------------------

    /**
     * Payload para envio de imagem com legenda opcional.
     *
     * @param phone   Número no formato internacional sem "+"
     * @param image   URL pública da imagem (HTTPS)
     * @param caption Legenda opcional exibida abaixo da imagem
     */
    @Getter
    @Builder
    public static class Imagem {
        private final String phone;
        private final String image;
        private final String caption;
    }

    // -------------------------------------------------------------------------
    // POST /send-audio
    // -------------------------------------------------------------------------

    /**
     * Payload para envio de áudio (aparece como mensagem de voz no WhatsApp).
     *
     * @param phone Número no formato internacional sem "+"
     * @param audio URL pública do arquivo de áudio (mp3/ogg)
     */
    @Getter
    @Builder
    public static class Audio {
        private final String phone;
        private final String audio;
    }

    // -------------------------------------------------------------------------
    // POST /send-video
    // -------------------------------------------------------------------------

    /**
     * Payload para envio de vídeo com legenda opcional.
     *
     * @param phone   Número no formato internacional sem "+"
     * @param video   URL pública do arquivo de vídeo (mp4)
     * @param caption Legenda opcional
     */
    @Getter
    @Builder
    public static class Video {
        private final String phone;
        private final String video;
        private final String caption;
    }
}