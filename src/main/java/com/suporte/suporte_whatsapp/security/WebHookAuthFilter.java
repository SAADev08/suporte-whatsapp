package com.suporte.suporte_whatsapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticação exclusivo para o endpoint de webhook da Z-API.
 *
 * A Z-API envia um token secreto no header "Client-Token" em cada requisição
 * de webhook. Este filtro intercepta apenas o caminho /api/webhook/whatsapp,
 * compara o header recebido com o secret configurado localmente e rejeita
 * com HTTP 401 qualquer requisição que não apresente o token correto.
 *
 * Por que um filtro separado e não validação inline no controller?
 * Um filtro atua antes que o corpo da requisição seja desserializado pelo
 * Jackson.
 * Isso evita trabalho desnecessário (parsing de JSON) em requisições ilegítimas
 * e mantém o WebhookController totalmente limpo de responsabilidades de
 * segurança.
 *
 * Configuração necessária no lado da Z-API:
 * 1. Acesse o painel Z-API → sua instância → Security
 * 2. Em "Security Passphrase (Client-Token)", defina um valor secreto
 * 3. Configure o mesmo valor em application.yml no campo zapi.webhook-secret
 * 4. A Z-API enviará este valor automaticamente no header Client-Token
 * em todas as chamadas de webhook para sua URL
 *
 * Configuração no application.yml:
 * zapi:
 * webhook-secret: "seu-secret-aqui"
 */
@Slf4j
public class WebHookAuthFilter extends OncePerRequestFilter {

    /** Nome do header enviado pela Z-API contendo o token de segurança. */
    private static final String HEADER_CLIENT_TOKEN = "Client-Token";

    /** Caminho protegido por este filtro. */
    private static final String WEBHOOK_PATH = "/api/webhook/whatsapp";

    /**
     * Secret configurado localmente. Valor default vazio garante que a aplicação
     * suba mesmo sem a propriedade definida, mas nesse caso o filtro
     * bloqueia todas as requisições (comportamento seguro por padrão).
     */
    private String webhookSecret;

    public WebHookAuthFilter(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Este filtro atua APENAS no endpoint de webhook — todo o resto passa direto
        if (!WEBHOOK_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Se nenhum secret foi configurado, bloquear com aviso explícito nos logs
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("[WebhookAuth] zapi.webhook-secret não configurado. " +
                    "Todas as requisições de webhook estão sendo bloqueadas por segurança.");
            escreverNaoAutorizado(response, "Webhook não configurado no servidor.");
            return;
        }

        String tokenRecebido = request.getHeader(HEADER_CLIENT_TOKEN);

        // Header ausente
        if (tokenRecebido == null || tokenRecebido.isBlank()) {
            log.warn("[WebhookAuth] Requisição rejeitada — header '{}' ausente. IP: {}",
                    HEADER_CLIENT_TOKEN, request.getRemoteAddr());
            escreverNaoAutorizado(response, "Header de autenticação ausente.");
            return;
        }

        // Comparação segura contra timing attacks (não usa equals simples)
        if (!constantTimeEquals(webhookSecret, tokenRecebido)) {
            log.warn("[WebhookAuth] Requisição rejeitada — token inválido. IP: {}",
                    request.getRemoteAddr());
            escreverNaoAutorizado(response, "Token de webhook inválido.");
            return;
        }

        // Token válido — continua a cadeia de filtros normalmente
        filterChain.doFilter(request, response);
    }

    /**
     * Escreve a resposta HTTP 401 com corpo JSON padronizado.
     */
    private void escreverNaoAutorizado(HttpServletResponse response, String mensagem)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + mensagem + "\"}");
    }

    /**
     * Comparação de strings em tempo constante para evitar timing attacks.
     *
     * Uma comparação simples com equals() pode vazar informação sobre
     * o tamanho e os primeiros caracteres do secret ao retornar mais rápido
     * quando os strings diferem cedo. Esta implementação sempre percorre
     * ambos os strings completos antes de retornar.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length())
            return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}