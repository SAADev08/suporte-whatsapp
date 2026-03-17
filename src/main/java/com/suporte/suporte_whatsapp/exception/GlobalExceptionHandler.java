package com.suporte.suporte_whatsapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handler global de exceções.
 *
 * Garante que nenhuma stack trace seja exposta ao cliente e que todas
 * as respostas de erro sigam um contrato JSON uniforme:
 *
 * {
 * "timestamp": "2024-01-15T10:30:00",
 * "status": 400,
 * "erro": "Requisição inválida",
 * "mensagem": "Campo 'email' é obrigatório",
 * "path": "/api/auth/login"
 * }
 *
 * Hierarquia de handlers (ordem importa — mais específico primeiro):
 *
 * 1. MethodArgumentNotValidException → 400 com lista de campos inválidos
 * 2. IllegalArgumentException → 400 mensagem da exceção
 * 3. IllegalStateException → 409 conflito de estado de negócio
 * 4. DataIntegrityViolationException → 409 violação de unicidade no banco
 * 5. BadCredentialsException → 401 credenciais inválidas
 * 6. DisabledException → 403 usuário inativo
 * 7. LockedException → 423 conta bloqueada (brute-force)
 * 8. AuthenticationException → 401 genérico de autenticação
 * 9. MethodArgumentTypeMismatchException → 400 tipo de parâmetro inválido
 * 10. Exception → 500 erro interno (sem detalhe)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. Validação de @Valid — retorna lista de campos com erro
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "inválido",
                        // Se o mesmo campo tiver mais de um erro, mantém o primeiro
                        (a, b) -> a,
                        LinkedHashMap::new));

        String mensagem = campos.entrySet().stream()
                .map(e -> "'" + e.getKey() + "': " + e.getValue())
                .collect(Collectors.joining("; "));

        log.warn("[Validação] Requisição inválida em {} — {}", req.getRequestURI(), mensagem);

        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", mensagem, req);
    }

    // =========================================================================
    // 2. IllegalArgumentException — erro de negócio / entidade não encontrada
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {

        log.warn("[Negócio] {} — path={}", ex.getMessage(), req.getRequestURI());
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), req);
    }

    // =========================================================================
    // 3. IllegalStateException — conflito de estado de negócio
    // Ex: tentar encerrar um chamado já encerrado
    // =========================================================================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest req) {

        log.warn("[Estado] {} — path={}", ex.getMessage(), req.getRequestURI());
        return resposta(HttpStatus.CONFLICT, "Operação não permitida", ex.getMessage(), req);
    }

    // =========================================================================
    // 4. DataIntegrityViolationException — violação de unicidade no banco
    // Ex: e-mail duplicado, telefone duplicado, CPF/CNPJ duplicado
    // =========================================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {

        // Extrair a constraint violada para dar mensagem útil sem expor detalhes do
        // banco
        String mensagem = traduzirConstraint(ex.getMessage());
        log.warn("[Integridade] Violação de constraint em {} — {}", req.getRequestURI(), mensagem);
        return resposta(HttpStatus.CONFLICT, "Conflito de dados", mensagem, req);
    }

    // =========================================================================
    // 5. BadCredentialsException — e-mail ou senha incorretos
    // =========================================================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {

        // Mensagem genérica intencional — não revela se o e-mail existe (segurança)
        log.warn("[Auth] Tentativa com credenciais inválidas — path={}", req.getRequestURI());
        return resposta(HttpStatus.UNAUTHORIZED, "Não autorizado",
                "E-mail ou senha inválidos.", req);
    }

    // =========================================================================
    // 6. DisabledException — usuário inativo
    // =========================================================================

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErroResponse> handleDisabled(
            DisabledException ex, HttpServletRequest req) {

        log.warn("[Auth] Tentativa de login com usuário inativo — path={}", req.getRequestURI());
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado",
                "Usuário inativo. Entre em contato com o administrador.", req);
    }

    // =========================================================================
    // 7. LockedException — conta bloqueada por excesso de tentativas (brute-force)
    // =========================================================================

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErroResponse> handleLocked(
            LockedException ex, HttpServletRequest req) {

        log.warn("[Segurança] Conta bloqueada por excesso de tentativas — path={}", req.getRequestURI());
        return resposta(HttpStatus.LOCKED, "Conta bloqueada", ex.getMessage(), req);
    }

    // =========================================================================
    // 8. AuthenticationException genérica — fallback de segurança
    // =========================================================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest req) {

        log.warn("[Auth] Falha de autenticação — {} — path={}", ex.getClass().getSimpleName(),
                req.getRequestURI());
        return resposta(HttpStatus.UNAUTHORIZED, "Não autorizado",
                "Falha na autenticação. Verifique suas credenciais.", req);
    }

    // =========================================================================
    // 9. MethodArgumentTypeMismatchException — UUID ou enum inválido na URL
    // Ex: GET /api/chamados/nao-e-um-uuid
    // =========================================================================

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {

        String mensagem = String.format(
                "Valor '%s' inválido para o parâmetro '%s'.", ex.getValue(), ex.getName());
        log.warn("[Validação] {} — path={}", mensagem, req.getRequestURI());
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido", mensagem, req);
    }

    // =========================================================================
    // 10. Exception genérica — último recurso, nunca expõe detalhes
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {

        // Loga com ERROR + stack trace completo para triagem interna
        log.error("[Erro interno] {} — path={}", ex.getMessage(), req.getRequestURI(), ex);

        // Retorna mensagem genérica ao cliente — sem detalhe de implementação
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.", req);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ResponseEntity<ErroResponse> resposta(
            HttpStatus status, String erro, String mensagem, HttpServletRequest req) {

        ErroResponse body = new ErroResponse(
                LocalDateTime.now(), status.value(), erro, mensagem, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Traduz nomes de constraint do PostgreSQL em mensagens legíveis ao usuário.
     *
     * Evita expor nomes internos de tabela/coluna enquanto ainda fornece
     * informação útil sobre o que causou o conflito.
     */
    private String traduzirConstraint(String msg) {
        if (msg == null)
            return "Registro duplicado ou inválido.";

        if (msg.contains("usuario_email_key") || msg.contains("uq_usuario_email"))
            return "Já existe um usuário cadastrado com este e-mail.";
        if (msg.contains("contato_telefone_key") || msg.contains("uq_contato_telefone"))
            return "Já existe um contato cadastrado com este telefone.";
        if (msg.contains("cliente_cpf_cnpj_key") || msg.contains("uq_cliente_cpf_cnpj"))
            return "Já existe um cliente cadastrado com este CPF/CNPJ.";
        if (msg.contains("uq_chat_message_id"))
            return "Mensagem duplicada.";

        return "Registro duplicado. Verifique os dados informados.";
    }

    // =========================================================================
    // DTO de resposta de erro
    // =========================================================================

    public record ErroResponse(
            LocalDateTime timestamp,
            int status,
            String erro,
            String mensagem,
            String path) {
    }
}