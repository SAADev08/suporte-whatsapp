package com.suporte.suporte_whatsapp.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Interceptor STOMP que valida o token JWT no momento do handshake WebSocket.
 *
 * O Spring Security protege rotas HTTP via filtros de servlet, mas a cadeia
 * de filtros não cobre mensagens STOMP após o handshake inicial. Este
 * interceptor preenche essa lacuna: ao receber um frame STOMP do tipo
 * CONNECT, extrai o header "Authorization", valida o JWT e popula o
 * SecurityContext com o usuário autenticado.
 *
 * Fluxo de autenticação WebSocket:
 *
 * Cliente Backend
 * | |
 * |-- STOMP CONNECT -------------->|
 * | Authorization: Bearer <jwt> |
 * | |-- WebSocketAuthInterceptor
 * | | valida JWT
 * | | popula SecurityContext
 * |<-- CONNECTED (ou ERROR) -------|
 * | |
 * |-- SUBSCRIBE /topic/mensagens ->| (usuário já autenticado)
 *
 * Como configurar no frontend (exemplo com @stomp/stompjs):
 *
 * const client = new Client({
 * brokerURL: 'ws://localhost:8080/ws/websocket',
 * connectHeaders: {
 * Authorization: 'Bearer ' + localStorage.getItem('token')
 * }
 * });
 *
 * Ou com SockJS + stomp.js:
 *
 * const socket = new SockJS('/ws');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect(
 * { Authorization: 'Bearer ' + token },
 * onConnected,
 * onError
 * );
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * Executado antes de cada mensagem ser enviada ao canal de entrada.
     * Apenas frames STOMP CONNECT são inspecionados — todos os outros
     * (SUBSCRIBE, SEND, DISCONNECT) passam direto, pois a autenticação
     * já foi estabelecida no CONNECT e fica no contexto da sessão.
     *
     * @throws org.springframework.messaging.MessageDeliveryException
     *                                                                se o token
     *                                                                estiver
     *                                                                ausente ou
     *                                                                inválido — o
     *                                                                STOMP broker
     *                                                                converte isso
     *                                                                em um frame
     *                                                                ERROR para o
     *                                                                cliente.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Não é um frame CONNECT — passa direto sem nenhuma inspeção
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        // Header ausente ou formato incorreto
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("[WebSocketAuth] Conexão STOMP rejeitada — header Authorization ausente ou inválido.");
            throw new IllegalArgumentException("Token JWT ausente na conexão WebSocket.");
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            String email = jwtUtil.extractUsername(jwt);

            if (email == null) {
                log.warn("[WebSocketAuth] Conexão STOMP rejeitada — token sem subject.");
                throw new IllegalArgumentException("Token JWT inválido.");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtUtil.isTokenValid(jwt, userDetails)) {
                log.warn("[WebSocketAuth] Conexão STOMP rejeitada — token expirado ou inválido para '{}'.", email);
                throw new IllegalArgumentException("Token JWT expirado ou inválido.");
            }

            // Autentica o usuário no contexto da sessão STOMP
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // Associa a autenticação ao accessor da sessão WebSocket
            // (persiste para todos os frames subsequentes da mesma conexão)
            accessor.setUser(authentication);

            // Também popula o SecurityContext para compatibilidade com
            // anotações @PreAuthorize em MessageMapping handlers, se utilizados
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("[WebSocketAuth] Conexão STOMP autenticada para '{}'.", email);

        } catch (IllegalArgumentException e) {
            // Relança para interromper a conexão com frame ERROR no cliente
            throw e;
        } catch (Exception e) {
            log.warn("[WebSocketAuth] Conexão STOMP rejeitada — erro ao validar token: {}", e.getMessage());
            throw new IllegalArgumentException("Falha na autenticação WebSocket.");
        }

        return message;
    }
}