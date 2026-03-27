package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.model.enums.NivelSla;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória do último nível SLA notificado por contato na triagem.
 *
 * <p>Mesmo ciclo de vida do {@link SlaNotificacaoCache}, mas keyed por
 * {@code contatoId} em vez de {@code chamadoId}, pois mensagens de triagem
 * ainda não possuem chamado vinculado.
 *
 * <h3>Limpeza</h3>
 * <ul>
 *   <li>{@link ChatService#responderTriagem} — ao analista responder na triagem</li>
 *   <li>{@link ChamadoService#criar} — ao converter mensagens de triagem em chamado formal</li>
 * </ul>
 */
@Component
public class TriagemNotificacaoCache {

    public record AlertaTriagemAtivo(
            UUID contatoId,
            String nomeContato,
            NivelSla nivel,
            String mensagem,
            LocalDateTime timestamp) {
    }

    private final ConcurrentHashMap<UUID, AlertaTriagemAtivo> cache = new ConcurrentHashMap<>();

    public boolean jaNotificadoNesseNivel(UUID contatoId, NivelSla nivel) {
        AlertaTriagemAtivo existing = cache.get(contatoId);
        return existing != null && nivel.equals(existing.nivel());
    }

    public void registrar(UUID contatoId, String nomeContato, NivelSla nivel, String mensagem) {
        cache.put(contatoId, new AlertaTriagemAtivo(contatoId, nomeContato, nivel, mensagem, LocalDateTime.now()));
    }

    /**
     * Remove o contato do cache. Deve ser chamado ao responder na triagem
     * ou ao criar um chamado a partir das mensagens do contato.
     * Idempotente.
     */
    public void limpar(UUID contatoId) {
        cache.remove(contatoId);
    }

    public List<AlertaTriagemAtivo> listarAtivos() {
        return List.copyOf(cache.values());
    }
}
