package com.suporte.suporte_whatsapp.security;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários do LoginAttemptService.
 *
 * Estratégia: puro JUnit + AssertJ, sem Spring context.
 * O service não tem dependências externas — instanciado diretamente no teste.
 *
 * Usa ReflectionTestUtils para injetar os @Value sem precisar do container,
 * permitindo testes rápidos (< 100ms) e parametrizáveis por cenário.
 */
@DisplayName("LoginAttemptService — controle de brute-force")
class LoginAttemptServiceTest {

    // Limites reduzidos para os testes ficarem rápidos
    private static final int MAX_TENTATIVAS = 3;
    private static final int BLOQUEIO_MINUTOS = 15;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(MAX_TENTATIVAS, BLOQUEIO_MINUTOS);
    }

    // =========================================================================
    // Cenários: conta livre (sem bloqueio)
    // =========================================================================

    @Nested
    @DisplayName("Conta livre")
    class ContaLivre {

        @Test
        @DisplayName("E-mail sem nenhuma tentativa não deve estar bloqueado")
        void emailSemTentativas_naoEstaBloqueado() {
            assertThat(service.estaBloqueado("usuario@empresa.com")).isFalse();
        }

        @Test
        @DisplayName("Abaixo do limite de tentativas não deve bloquear")
        void abaixoDoLimite_naoBloqueia() {
            registrarFalhas("analista@empresa.com", MAX_TENTATIVAS - 1);

            assertThat(service.estaBloqueado("analista@empresa.com")).isFalse();
        }

        @Test
        @DisplayName("minutosRestantes deve ser 0 para conta não bloqueada")
        void contaNaoBloqueada_minutosRestantesZero() {
            assertThat(service.minutosRestantes("livre@empresa.com")).isZero();
        }
    }

    // =========================================================================
    // Cenários: bloqueio por excesso de tentativas
    // =========================================================================

    @Nested
    @DisplayName("Bloqueio por excesso de tentativas")
    class BloqueioExcessoTentativas {

        @Test
        @DisplayName("Exatamente no limite deve bloquear a conta")
        void noLimite_bloqueiaAConta() {
            registrarFalhas("alvo@empresa.com", MAX_TENTATIVAS);

            assertThat(service.estaBloqueado("alvo@empresa.com")).isTrue();
        }

        @Test
        @DisplayName("Acima do limite continua bloqueado")
        void acimadoLimite_continuaBloqueado() {
            registrarFalhas("alvo@empresa.com", MAX_TENTATIVAS + 5);

            assertThat(service.estaBloqueado("alvo@empresa.com")).isTrue();
        }

        @Test
        @DisplayName("minutosRestantes deve ser > 0 quando bloqueado")
        void bloqueado_minutosRestantesPositivo() {
            registrarFalhas("alvo@empresa.com", MAX_TENTATIVAS);

            assertThat(service.minutosRestantes("alvo@empresa.com")).isPositive();
        }

        @Test
        @DisplayName("minutosRestantes não deve exceder o tempo de bloqueio configurado")
        void minutosRestantes_naoExcedeBloqueioConfigurado() {
            registrarFalhas("alvo@empresa.com", MAX_TENTATIVAS);

            // +1 porque o método arredonda para cima o minuto parcial
            assertThat(service.minutosRestantes("alvo@empresa.com"))
                    .isLessThanOrEqualTo(BLOQUEIO_MINUTOS + 1);
        }
    }

    // =========================================================================
    // Cenários: liberação após sucesso
    // =========================================================================

    @Nested
    @DisplayName("Liberação após login bem-sucedido")
    class LiberacaoAposSucesso {

        @Test
        @DisplayName("Login bem-sucedido deve zerar o contador e liberar a conta")
        void sucessoAposMultiplasFalhas_liberaConta() {
            registrarFalhas("usuario@empresa.com", MAX_TENTATIVAS - 1);
            service.registrarSucesso("usuario@empresa.com");

            assertThat(service.estaBloqueado("usuario@empresa.com")).isFalse();
            assertThat(service.minutosRestantes("usuario@empresa.com")).isZero();
        }

        @Test
        @DisplayName("Após sucesso, nova série de falhas deve recontagem do zero")
        void aposSuccesso_novasSerieDeFalhasRecontaDoZero() {
            // Primeira série — não bloqueou
            registrarFalhas("usuario@empresa.com", MAX_TENTATIVAS - 1);
            service.registrarSucesso("usuario@empresa.com");

            // Segunda série — deve recontagem limpa
            registrarFalhas("usuario@empresa.com", MAX_TENTATIVAS - 1);

            // Ainda abaixo do limite com contagem resetada
            assertThat(service.estaBloqueado("usuario@empresa.com")).isFalse();
        }

        @Test
        @DisplayName("Sucesso em conta sem tentativas anteriores não deve causar erro")
        void sucessoSemTentativasAnteriores_semErro() {
            assertThatCode(() -> service.registrarSucesso("nenhuma@empresa.com"))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // Cenários: expiração automática do bloqueio
    // =========================================================================

    @Nested
    @DisplayName("Expiração automática do bloqueio")
    class ExpiracaoAutomatica {

        @Test
        @DisplayName("Bloqueio deve expirar após o tempo configurado")
        void bloqueio_expiraAposTempoConfigurado() {
            // Cria service com bloqueio de apenas 1ms para testar expiração
            LoginAttemptService serviceCurtoBloqueio = new LoginAttemptService(MAX_TENTATIVAS, 0);
            // bloqueio-minutos=0 → bloqueioMs=0 → expira imediatamente

            registrarFalhas(serviceCurtoBloqueio, "alvo@empresa.com", MAX_TENTATIVAS);

            // Sem sleep — o bloqueio com 0ms já está expirado
            assertThat(serviceCurtoBloqueio.estaBloqueado("alvo@empresa.com")).isFalse();
        }

        @Test
        @DisplayName("Após expiração, minutosRestantes deve retornar 0")
        void aposExpiracao_minutosRestantesZero() {
            LoginAttemptService serviceCurtoBloqueio = new LoginAttemptService(MAX_TENTATIVAS, 0);

            registrarFalhas(serviceCurtoBloqueio, "alvo@empresa.com", MAX_TENTATIVAS);

            assertThat(serviceCurtoBloqueio.minutosRestantes("alvo@empresa.com")).isZero();
        }
    }

    // =========================================================================
    // Cenários: case insensitivity e normalização de e-mail
    // =========================================================================

    @Nested
    @DisplayName("Normalização de e-mail")
    class NormalizacaoEmail {

        @Test
        @DisplayName("E-mail em maiúsculas e minúsculas deve contar para o mesmo contador")
        void emailCaseInsensitive_mesmoContador() {
            service.registrarFalha("Usuario@Empresa.COM");
            service.registrarFalha("USUARIO@EMPRESA.COM");
            service.registrarFalha("usuario@empresa.com");

            // 3 falhas com variações de case = bloqueio atingido (MAX = 3)
            assertThat(service.estaBloqueado("usuario@empresa.com")).isTrue();
        }

        @Test
        @DisplayName("Sucesso com e-mail em case diferente deve liberar a conta")
        void sucessoCaseDiferente_liberaConta() {
            registrarFalhas("usuario@empresa.com", MAX_TENTATIVAS - 1);
            service.registrarSucesso("USUARIO@EMPRESA.COM");

            assertThat(service.estaBloqueado("usuario@empresa.com")).isFalse();
        }

        @Test
        @DisplayName("E-mails de usuários diferentes não devem interferir entre si")
        void emailsDiferentes_naointerfere() {
            registrarFalhas("atacante@mal.com", MAX_TENTATIVAS);

            assertThat(service.estaBloqueado("inocente@empresa.com")).isFalse();
        }
    }

    // =========================================================================
    // Cenários: entradas inválidas / edge cases
    // =========================================================================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("registrarFalha com e-mail nulo não deve lançar exceção")
        void falhaComEmailNulo_semExcecao() {
            assertThatCode(() -> service.registrarFalha(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("estaBloqueado com e-mail nulo deve retornar false")
        void bloqueadoComEmailNulo_retornaFalse() {
            assertThat(service.estaBloqueado(null)).isFalse();
        }

        @Test
        @DisplayName("minutosRestantes com e-mail nulo deve retornar 0")
        void minutosRestantesComEmailNulo_retornaZero() {
            assertThat(service.minutosRestantes(null)).isZero();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void registrarFalhas(String email, int quantidade) {
        registrarFalhas(service, email, quantidade);
    }

    private void registrarFalhas(LoginAttemptService svc, String email, int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            svc.registrarFalha(email);
        }
    }
}
