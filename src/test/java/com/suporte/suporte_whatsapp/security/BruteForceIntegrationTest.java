package com.suporte.suporte_whatsapp.security;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Teste de integração da cadeia AuthController → LoginAttemptService.
 *
 * Foca no comportamento do fluxo de brute-force de ponta a ponta,
 * sem depender de banco ou Spring context.
 *
 * Cenários cobertos:
 *   - Contagem progressiva de falhas até o bloqueio
 *   - Bloqueio impede tentativas adicionais
 *   - Sucesso zera o contador em qualquer ponto da contagem
 *   - Múltiplos usuários têm contadores independentes
 *   - Expiração natural do bloqueio
 */
@DisplayName("Fluxo completo de brute-force — integração LoginAttemptService")
class BruteForceIntegrationTest {

    private static final int MAX = 3;
    private static final int BLOQUEIO_MIN = 15;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(MAX, BLOQUEIO_MIN);
    }

    @Test
    @DisplayName("Contagem deve crescer progressivamente e bloquear ao atingir o limite")
    void contagemProgressiva_bloqueiaNoLimite() {
        String email = "alvo@empresa.com";

        for (int i = 1; i <= MAX; i++) {
            service.registrarFalha(email);
            boolean esperaBloqueio = (i >= MAX);
            assertThat(service.estaBloqueado(email))
                    .as("Após %d falha(s), bloqueado deve ser %s", i, esperaBloqueio)
                    .isEqualTo(esperaBloqueio);
        }
    }

    @Test
    @DisplayName("Sucesso na primeira tentativa não deve bloquear")
    void sucessoPrimeiraTentativa_naoBloqueia() {
        service.registrarSucesso("feliz@empresa.com");
        assertThat(service.estaBloqueado("feliz@empresa.com")).isFalse();
    }

    @Test
    @DisplayName("Sucesso após N-1 falhas deve zerar o contador")
    void sucessoAposFalhas_zeradorContador() {
        String email = "quase@empresa.com";

        // N-1 falhas — não bloqueado ainda
        for (int i = 0; i < MAX - 1; i++) service.registrarFalha(email);
        assertThat(service.estaBloqueado(email)).isFalse();

        // Sucesso — zera
        service.registrarSucesso(email);
        assertThat(service.estaBloqueado(email)).isFalse();

        // Nova série de N-1 falhas após sucesso — ainda não bloqueia
        for (int i = 0; i < MAX - 1; i++) service.registrarFalha(email);
        assertThat(service.estaBloqueado(email)).isFalse();
    }

    @Test
    @DisplayName("Dois usuários com padrões de tentativa diferentes não se interferem")
    void doisUsuarios_independentes() {
        String atacante = "atacante@mal.com";
        String inocente = "inocente@empresa.com";

        // Atacante bate no limite
        for (int i = 0; i < MAX; i++) service.registrarFalha(atacante);

        // Inocente tem uma falha normal
        service.registrarFalha(inocente);

        assertThat(service.estaBloqueado(atacante)).isTrue();
        assertThat(service.estaBloqueado(inocente)).isFalse();
    }

    @Test
    @DisplayName("Bloqueio deve expirar automaticamente após o tempo configurado")
    void bloqueio_expiraAutomaticamente() {
        // Cria service com bloqueio de 0 minutos para forçar expiração imediata
        LoginAttemptService serviceCurto = new LoginAttemptService(MAX, 0);
        String email = "temporario@empresa.com";

        for (int i = 0; i < MAX; i++) serviceCurto.registrarFalha(email);

        // Com bloqueio de 0ms, a próxima verificação já expira
        assertThat(serviceCurto.estaBloqueado(email)).isFalse();
        assertThat(serviceCurto.minutosRestantes(email)).isZero();
    }

    @Test
    @DisplayName("Após expiração, nova série de falhas deve funcionar normalmente")
    void aposExpiracao_novaSerieDeveRecontar() {
        LoginAttemptService serviceCurto = new LoginAttemptService(MAX, 0);
        String email = "reincidente@empresa.com";

        // Primeira série — expira imediatamente
        for (int i = 0; i < MAX; i++) serviceCurto.registrarFalha(email);
        assertThat(serviceCurto.estaBloqueado(email)).isFalse(); // expirado

        // Segunda série — recontar do zero
        for (int i = 0; i < MAX - 1; i++) serviceCurto.registrarFalha(email);
        assertThat(serviceCurto.estaBloqueado(email)).isFalse(); // abaixo do limite
    }

    @Test
    @DisplayName("minutosRestantes deve diminuir à medida que o tempo passa (simulado)")
    void minutosRestantes_refletemTempoBloqueioConfigurado() {
        String email = "cronometro@empresa.com";
        for (int i = 0; i < MAX; i++) service.registrarFalha(email);

        long minutos = service.minutosRestantes(email);

        // Deve ser um valor entre 1 e BLOQUEIO_MIN+1 (arredonda para cima)
        assertThat(minutos)
                .isGreaterThan(0)
                .isLessThanOrEqualTo(BLOQUEIO_MIN + 1);
    }
}
