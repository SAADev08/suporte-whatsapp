package com.suporte.suporte_whatsapp.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suporte.suporte_whatsapp.controller.AuthController;
import com.suporte.suporte_whatsapp.dto.LoginRequest;
import com.suporte.suporte_whatsapp.repository.UsuarioRepository;
import com.suporte.suporte_whatsapp.security.JwtUtil;
import com.suporte.suporte_whatsapp.security.LoginAttemptService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes do GlobalExceptionHandler via MockMvc standalone.
 *
 * Estratégia: MockMvc sem Spring context completo (@WebMvcTest seria mais
 * pesado
 * e exigiria configurar Security). Aqui montamos apenas o controller + handler
 * manualmente — startup em ~200ms vs ~3s do contexto completo.
 *
 * Cada teste verifica:
 * 1. O status HTTP correto
 * 2. O campo "erro" no JSON
 * 3. O campo "mensagem" com conteúdo esperado
 * 4. A ausência de stack trace na resposta
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — respostas de erro padronizadas")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules(); // registra JavaTimeModule para LocalDateTime

    @Mock
    private AuthenticationManager authManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // Apenas monta o MockMvc — sem stubbing aqui.
        // Cada grupo configura seus próprios stubs para evitar
        // UnnecessaryStubbingException.
        // Os testes de ValidacaoCampos falham no dispatcher do @Valid antes de chegar
        // ao controller, por isso o stub de estaBloqueado() nunca seria invocado lá.
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =========================================================================
    // 400 — Validação de campos (@Valid)
    // Estes testes falham no dispatcher ANTES de chegar ao controller.
    // NÃO precisam do stub de loginAttemptService.
    // =========================================================================

    @Nested
    @DisplayName("400 — Validação de campos")
    class ValidacaoCampos {

        @Test
        @DisplayName("Corpo vazio deve retornar 400 com lista de campos inválidos")
        void corpoVazio_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.erro").value("Requisição inválida"))
                    .andExpect(jsonPath("$.mensagem").exists())
                    .andExpect(jsonPath("$.path").value("/api/auth/login"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("E-mail com formato inválido deve retornar 400")
        void emailFormatoInvalido_retorna400() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("nao-e-um-email");
            req.setSenha("qualquerSenha");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.mensagem").isNotEmpty());
        }

        @Test
        @DisplayName("Resposta de validação nunca deve conter 'stackTrace' ou 'exception'")
        void respostaValidacao_semStackTrace() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.stackTrace").doesNotExist())
                    .andExpect(jsonPath("$.exception").doesNotExist())
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
    }

    // =========================================================================
    // 401 — Credenciais inválidas
    // =========================================================================

    @Nested
    @DisplayName("401 — Credenciais inválidas")
    class CredenciaisInvalidas {

        @BeforeEach
        void stub() {
            when(loginAttemptService.estaBloqueado(any())).thenReturn(false);
        }

        @Test
        @DisplayName("BadCredentialsException deve retornar 401 com mensagem genérica")
        void badCredentials_retorna401() throws Exception {
            when(authManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginValido()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.erro").value("Não autorizado"))
                    .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
        }

        @Test
        @DisplayName("Mensagem de 401 nunca deve revelar se o e-mail existe")
        void mensagem401_naoRevelaSeEmailExiste() throws Exception {
            when(authManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginValido()))
                    .andExpect(status().isUnauthorized())
                    // Mensagem não pode conter "usuário não encontrado" ou "e-mail não existe"
                    .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
        }
    }

    // =========================================================================
    // 403 — Usuário inativo
    // =========================================================================

    @Nested
    @DisplayName("403 — Usuário inativo")
    class UsuarioInativo {

        @BeforeEach
        void stub() {
            when(loginAttemptService.estaBloqueado(any())).thenReturn(false);
        }

        @Test
        @DisplayName("DisabledException deve retornar 403")
        void usuarioInativo_retorna403() throws Exception {
            when(authManager.authenticate(any()))
                    .thenThrow(new DisabledException("User is disabled"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginValido()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.erro").value("Acesso negado"))
                    .andExpect(jsonPath("$.mensagem").value(
                            "Usuário inativo. Entre em contato com o administrador."));
        }
    }

    // =========================================================================
    // 423 — Conta bloqueada (brute-force)
    // =========================================================================

    @Nested
    @DisplayName("423 — Conta bloqueada")
    class ContaBloqueada {

        @Test
        @DisplayName("Conta bloqueada deve retornar 423 com tempo restante")
        void contaBloqueada_retorna423() throws Exception {
            when(loginAttemptService.estaBloqueado("analista@empresa.com")).thenReturn(true);
            when(loginAttemptService.minutosRestantes("analista@empresa.com")).thenReturn(12L);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginComEmail("analista@empresa.com")))
                    .andExpect(status().isLocked())
                    .andExpect(jsonPath("$.status").value(423))
                    .andExpect(jsonPath("$.erro").value("Conta bloqueada"))
                    .andExpect(jsonPath("$.mensagem").value(
                            org.hamcrest.Matchers.containsString("12 minuto")));
        }

        @Test
        @DisplayName("Conta bloqueada não deve chamar o AuthenticationManager")
        void contaBloqueada_naoChamaAuthManager() throws Exception {
            when(loginAttemptService.estaBloqueado(any())).thenReturn(true);
            when(loginAttemptService.minutosRestantes(any())).thenReturn(5L);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginValido()))
                    .andExpect(status().isLocked());

            // Garante que o AuthenticationManager não foi consultado
            // (não deve fazer consulta ao banco enquanto bloqueado)
            verifyNoInteractions(authManager);
        }
    }

    // =========================================================================
    // Estrutura da resposta de erro
    // =========================================================================

    @Nested
    @DisplayName("Estrutura da resposta de erro")
    class EstruturaResposta {

        @BeforeEach
        void stub() {
            when(loginAttemptService.estaBloqueado(any())).thenReturn(false);
        }

        @Test
        @DisplayName("Toda resposta de erro deve ter os campos obrigatórios")
        void respostaErro_temCamposObrigatorios() throws Exception {
            when(authManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginValido()))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.erro").exists())
                    .andExpect(jsonPath("$.mensagem").exists())
                    .andExpect(jsonPath("$.path").exists());
        }

        @Test
        @DisplayName("path na resposta deve corresponder ao endpoint chamado")
        void path_correspondeAoEndpointChamado() throws Exception {
            when(authManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("x"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginValido()))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String loginValido() throws Exception {
        return loginComEmail("analista@empresa.com");
    }

    private String loginComEmail(String email) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setSenha("SenhaForte@123");
        return mapper.writeValueAsString(req);
    }
}