package com.suporte.suporte_whatsapp.dto;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope imutável que envolve qualquer payload emitido via WebSocket,
 * adicionando os campos de rastreamento necessários para deduplicação
 * e ordenação no frontend
 *
 * <h3>Campos</h3>
 * <ul>
 * <li>{@code eventId} — UUID v4 gerado no momento da emissão. O frontend
 * armazena IDs já processados em um {@code Set} e descarta re-entregas
 * do broker STOMP durante reconexões.</li>
 * <li>{@code serverTimestamp} — epoch em milissegundos no momento da emissão.
 * Permite ao frontend ordenar eventos e descartar atualizações de estado
 * mais antigas que a versão já aplicada (versionamento por timestamp).</li>
 * <li>{@code payload} — o objeto original sem modificação.</li>
 * </ul>
 *
 * <h3>Uso</h3>
 * 
 * <pre>{@code
 * ws.convertAndSend("/topic/mensagens", WsEnvelope.of(ChatResponse.from(msg)));
 * }</pre>
 *
 * @param <T> tipo do payload encapsulado
 */
@Getter
public final class WsEnvelope<T> {

    /** Identificador único desta emissão — para deduplicação no cliente. */
    private final String eventId;

    /**
     * Momento da emissão em epoch milissegundos (UTC).
     * Usar {@code long} em vez de {@code LocalDateTime} garante que o Jackson
     * serialize como número simples, evitando problemas de fuso horário no
     * frontend ao reconstruir a ordem dos eventos.
     */
    private final long serverTimestamp;

    /** Payload original — não modificado. */
    private final T payload;

    private WsEnvelope(T payload) {
        this.eventId = UUID.randomUUID().toString();
        this.serverTimestamp = Instant.now().toEpochMilli();
        this.payload = payload;
    }

    /**
     * Fábrica estática — gera {@code eventId} e {@code serverTimestamp}
     * no momento da chamada.
     *
     * @param payload objeto a encapsular
     * @param <T>     tipo do payload
     * @return novo envelope com IDs únicos para esta emissão
     */
    public static <T> WsEnvelope<T> of(T payload) {
        return new WsEnvelope<>(payload);
    }
}