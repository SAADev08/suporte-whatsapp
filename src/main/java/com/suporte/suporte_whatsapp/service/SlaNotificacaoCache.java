package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.model.enums.NivelSla;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória do último nível SLA notificado por chamado.
 *
 * <p>
 * Extraído como {@code @Component} separado para que tanto o
 * {@link SlaScheduler} (escritor) quanto o {@link ChatService} e o
 * {@link ChamadoService} (leitores/limpadores) possam usá-lo sem criar
 * uma dependência circular entre os próprios services.
 *
 * <h3>Ciclo de vida de uma entrada</h3>
 * <ol>
 * <li>{@code registrar} — chamado pelo {@link SlaScheduler} ao emitir
 * um alerta. Guarda o nível emitido para aquele chamado.</li>
 * <li>{@code jaNotificadoNesseNivel} — consultado antes de emitir:
 * se o chamado já recebeu aquele nível neste ciclo, o evento é
 * suprimido, evitando alertas repetidos a cada minuto.</li>
 * <li>{@code limpar} — chamado pelo {@link ChatService} na primeira
 * resposta do analista e pelo {@link ChamadoService} ao encerrar
 * o chamado. Remove a entrada para que futuros alertas (caso o
 * chamado seja reaberto ou outro surgir) sejam entregues normalmente.</li>
 * </ol>
 *
 * <h3>Thread-safety</h3>
 * {@link ConcurrentHashMap} garante acesso seguro em ambiente com múltiplos
 * analistas e o scheduler rodando em thread própria sem necessidade de
 * sincronização adicional.
 *
 * <h3>Persistência</h3>
 * O cache é intencional <strong>em memória</strong>: em caso de restart da
 * aplicação o pior cenário é um único alerta "extra" por chamado pendente,
 * o que é aceitável. Adicionar persistência introduziria complexidade
 * desnecessária para o volume esperado.
 */
@Component
public class SlaNotificacaoCache {

    public record AlertaSlaAtivo(
            UUID chamadoId,
            NivelSla nivel,
            String mensagem,
            LocalDateTime timestamp) {
    }

    private final ConcurrentHashMap<UUID, AlertaSlaAtivo> cache = new ConcurrentHashMap<>();

    /**
     * Retorna {@code true} se o chamado já foi notificado com este nível SLA
     * e o alerta não deve ser reenviado.
     *
     * @param chamadoId identificador do chamado
     * @param nivel     nível SLA a verificar
     */
    public boolean jaNotificadoNesseNivel(UUID chamadoId, NivelSla nivel) {
        AlertaSlaAtivo existing = cache.get(chamadoId);

        return existing != null && nivel.equals((existing.nivel()));
    }

    /**
     * Registra o nível SLA emitido para um chamado.
     * Substitui qualquer nível anterior (a escala é sempre crescente:
     * ALERTA → CRITICO → ESCALADO, então uma substituição é sempre
     * uma escalada — nunca uma regressão).
     *
     * @param chamadoId identificador do chamado
     * @param nivel     nível emitido
     */
    public void registrar(UUID chamadoId, NivelSla nivel, String mensagem) {
        cache.put(chamadoId, new AlertaSlaAtivo(chamadoId, nivel, mensagem, LocalDateTime.now()));
    }

    /**
     * Remove o chamado do cache.
     * Deve ser chamado quando o chamado for respondido ou encerrado,
     * liberando a entrada para que alertas futuros (reaberturas) sejam
     * entregues normalmente.
     *
     * Idempotente: não lança exceção se o chamadoId não estiver no cache.
     *
     * @param chamadoId identificador do chamado a remover
     */
    public void limpar(UUID chamadoId) {
        cache.remove(chamadoId);
    }

    public List<AlertaSlaAtivo> listarAtivos() {
        return List.copyOf(cache.values());
    }
}