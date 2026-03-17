package com.suporte.suporte_whatsapp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controle de tentativas de login para proteção contra brute-force.
 *
 * ── Estratégia de armazenamento ──────────────────────────────────────────────
 *
 * Usa ConcurrentHashMap em memória por dois motivos deliberados:
 *
 * 1. O projeto não possui Redis nem outro cache distribuído. Adicionar
 * infraestrutura nova só para brute-force seria over-engineering para
 * este estágio do MVP.
 *
 * 2. O reinício da aplicação limpa os contadores — comportamento aceitável
 * para um sistema com poucos usuários simultâneos.
 *
 * Implicação conhecida: em deploy com múltiplas instâncias (load balancer),
 * contadores não são compartilhados entre pods. Se isso se tornar requisito,
 * a troca para Redis é trivial — basta substituir o ConcurrentHashMap por
 * um RedisTemplate mantendo a mesma interface pública.
 *
 * ── Funcionamento ────────────────────────────────────────────────────────────
 *
 * - Cada e-mail tem um registro com: contagem de falhas e timestamp da última.
 * - Ao atingir o limite, o acesso é bloqueado por
 * {bruteForce.bloqueio-minutos}.
 * - Após o tempo de bloqueio, a contagem é zerada automaticamente na próxima
 * tentativa (sem necessidade de job de limpeza).
 * - Login bem-sucedido sempre limpa o contador.
 *
 * ── Configuração (application.yml) ───────────────────────────────────────────
 *
 * brute-force:
 * max-tentativas: 5
 * bloqueio-minutos: 15
 */
@Service
@Slf4j
public class LoginAttemptService {

    private final int maxTentativas;
    private final long bloqueioMs;

    // Chave: e-mail do usuário (lowercase). Valor: estado de tentativas.
    private final ConcurrentHashMap<String, TentativaState> tentativas = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${brute-force.max-tentativas:5}") int maxTentativas,
            @Value("${brute-force.bloqueio-minutos:15}") int bloqueioMinutos) {
        this.maxTentativas = maxTentativas;
        this.bloqueioMs = (long) bloqueioMinutos * 60 * 1000;
    }

    // =========================================================================
    // API pública
    // =========================================================================

    /**
     * Registra uma tentativa de login com falha para o e-mail informado.
     * Deve ser chamado APÓS capturar BadCredentialsException.
     */
    public void registrarFalha(String email) {
        String chave = normalizar(email);
        tentativas.compute(chave, (k, estado) -> {
            if (estado == null || estaExpirado(estado)) {
                // Primeira falha ou bloqueio já expirou — inicia nova contagem
                return new TentativaState(1, Instant.now());
            }
            return new TentativaState(estado.contador() + 1, Instant.now());
        });

        TentativaState atual = tentativas.get(chave);
        if (atual != null && atual.contador() >= maxTentativas) {
            log.warn("[BruteForce] Conta bloqueada após {} tentativas — e-mail terminado em {}",
                    maxTentativas, sufixoEmail(email));
        } else if (atual != null) {
            log.warn("[BruteForce] Falha de login {}/{} — e-mail terminado em {}",
                    atual.contador(), maxTentativas, sufixoEmail(email));
        }
    }

    /**
     * Limpa o contador após login bem-sucedido.
     * Deve ser chamado APÓS autenticação confirmada pelo AuthenticationManager.
     */
    public void registrarSucesso(String email) {
        tentativas.remove(normalizar(email));
    }

    /**
     * Verifica se o e-mail está atualmente bloqueado.
     *
     * @return true se bloqueado, false se livre para tentar
     */
    public boolean estaBloqueado(String email) {
        TentativaState estado = tentativas.get(normalizar(email));
        if (estado == null)
            return false;

        // Se o bloqueio já expirou, limpa e libera
        if (estaExpirado(estado)) {
            tentativas.remove(normalizar(email));
            return false;
        }

        return estado.contador() >= maxTentativas;
    }

    /**
     * Retorna quantos minutos restam de bloqueio para o e-mail informado.
     * Útil para compor a mensagem de erro com o tempo exato.
     *
     * @return minutos restantes, ou 0 se não bloqueado
     */
    public long minutosRestantes(String email) {
        TentativaState estado = tentativas.get(normalizar(email));
        if (estado == null || estado.contador() < maxTentativas)
            return 0;

        long decorrido = Instant.now().toEpochMilli() - estado.ultimaTentativa().toEpochMilli();
        long restante = bloqueioMs - decorrido;
        return restante > 0 ? (restante / 60_000) + 1 : 0;
    }

    // =========================================================================
    // Helpers internos
    // =========================================================================

    private boolean estaExpirado(TentativaState estado) {
        long decorrido = Instant.now().toEpochMilli() - estado.ultimaTentativa().toEpochMilli();
        return decorrido >= bloqueioMs;
    }

    private String normalizar(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    /**
     * Retorna apenas o domínio do e-mail para logs — não loga o endereço completo
     * (LGPD).
     * Ex: "usuario@empresa.com" → "@empresa.com"
     */
    private String sufixoEmail(String email) {
        if (email == null)
            return "desconhecido";
        int at = email.indexOf('@');
        return at >= 0 ? email.substring(at) : "desconhecido";
    }

    // =========================================================================
    // Estado imutável por e-mail
    // =========================================================================

    private record TentativaState(int contador, Instant ultimaTentativa) {
    }
}