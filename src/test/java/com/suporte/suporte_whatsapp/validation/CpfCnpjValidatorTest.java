package com.suporte.suporte_whatsapp.validation;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do CpfCnpjValidator.
 *
 * Estratégia: instancia o validator diretamente (sem Spring/Hibernate
 * Validator),
 * passando null como ConstraintValidatorContext — o validator não o utiliza.
 *
 * Cobre:
 * - CPFs válidos com e sem máscara
 * - CNPJs válidos com e sem máscara
 * - Sequências repetidas (matematicamente válidas, mas rejeitadas pela RF)
 * - Dígitos verificadores incorretos
 * - Comprimentos errados
 * - Entradas nulas e em branco
 */
@DisplayName("CpfCnpjValidator — algoritmo de validação")
class CpfCnpjValidatorTest {

    private final CpfCnpjValidator validator = new CpfCnpjValidator();

    // =========================================================================
    // CPF válido
    // =========================================================================

    @Nested
    @DisplayName("CPF — válidos")
    class CpfValidos {

        @ParameterizedTest(name = "{0}")
        @DisplayName("CPF válido deve passar na validação")
        @ValueSource(strings = {
                "529.982.247-25", // com máscara
                "52998224725", // sem máscara
                "111.444.777-35", // outro CPF válido com máscara
                "11144477735", // mesmo sem máscara
                "153.509.460-56", // CPF válido adicional
                "000.000.001-91", // começa com zeros (válido)
        })
        void cpfValido(String cpf) {
            assertThat(validator.isValid(cpf, null))
                    .as("CPF '%s' deveria ser válido", cpf)
                    .isTrue();
        }
    }

    // =========================================================================
    // CPF inválido
    // =========================================================================

    @Nested
    @DisplayName("CPF — inválidos")
    class CpfInvalidos {

        @ParameterizedTest(name = "{0}")
        @DisplayName("CPF com dígito verificador errado deve ser rejeitado")
        @ValueSource(strings = {
                "529.982.247-26", // último dígito errado
                "529.982.247-35", // penúltimo dígito errado
                "111.111.111-11", // sequência repetida
                "000.000.000-00", // sequência repetida de zeros
                "123.456.789-00", // dígitos incorretos
                "999.999.999-99", // sequência repetida
        })
        void cpfInvalido(String cpf) {
            assertThat(validator.isValid(cpf, null))
                    .as("CPF '%s' deveria ser inválido", cpf)
                    .isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("CPF com comprimento errado deve ser rejeitado")
        @ValueSource(strings = {
                "5299822472", // 10 dígitos (falta 1)
                "529982247250", // 12 dígitos (sobra 1)
                "123", // muito curto
        })
        void cpfComprimentoErrado(String cpf) {
            assertThat(validator.isValid(cpf, null)).isFalse();
        }
    }

    // =========================================================================
    // CNPJ válido
    // =========================================================================

    @Nested
    @DisplayName("CNPJ — válidos")
    class CnpjValidos {

        @ParameterizedTest(name = "{0}")
        @DisplayName("CNPJ válido deve passar na validação")
        @ValueSource(strings = {
                "11.222.333/0001-81", // com máscara
                "11222333000181", // sem máscara
                "60.701.190/0001-04", // Banco do Brasil (CNPJ público)
                "60701190000104", // sem máscara
                "00.000.000/0001-91", // começa com zeros (válido)
        })
        void cnpjValido(String cnpj) {
            assertThat(validator.isValid(cnpj, null))
                    .as("CNPJ '%s' deveria ser válido", cnpj)
                    .isTrue();
        }
    }

    // =========================================================================
    // CNPJ inválido
    // =========================================================================

    @Nested
    @DisplayName("CNPJ — inválidos")
    class CnpjInvalidos {

        @ParameterizedTest(name = "{0}")
        @DisplayName("CNPJ com dígito verificador errado deve ser rejeitado")
        @ValueSource(strings = {
                "11.222.333/0001-82", // último dígito errado
                "11.222.333/0001-91", // penúltimo dígito errado
                "11.111.111/1111-11", // sequência repetida
                "00.000.000/0000-00", // sequência repetida de zeros
                "12.345.678/0001-00", // dígitos incorretos
        })
        void cnpjInvalido(String cnpj) {
            assertThat(validator.isValid(cnpj, null))
                    .as("CNPJ '%s' deveria ser inválido", cnpj)
                    .isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("CNPJ com comprimento errado deve ser rejeitado")
        @ValueSource(strings = {
                "1122233300018", // 13 dígitos (falta 1)
                "112223330001810", // 15 dígitos (sobra 1)
                "1234567", // muito curto
        })
        void cnpjComprimentoErrado(String cnpj) {
            assertThat(validator.isValid(cnpj, null)).isFalse();
        }
    }

    // =========================================================================
    // Formato e máscara
    // =========================================================================

    @Nested
    @DisplayName("Normalização de formato")
    class Formato {

        @Test
        @DisplayName("CPF com espaços deve ser aceito após normalização")
        void cpfComEspacos() {
            assertThat(validator.isValid("529 982 247 25", null)).isTrue();
        }

        @Test
        @DisplayName("CNPJ com formatação mista deve ser aceito")
        void cnpjFormatacaoMista() {
            // Remove qualquer não-dígito — formato incomum mas tolerado
            assertThat(validator.isValid("11 222 333 0001 81", null)).isTrue();
        }

        @Test
        @DisplayName("CPF com máscara parcial deve ser tratado corretamente")
        void cpfMascaraParcial() {
            // 529.98224725 → remove ponto → 52998224725 (11 dígitos, válido)
            assertThat(validator.isValid("529.98224725", null)).isTrue();
        }
    }

    // =========================================================================
    // Sequências repetidas
    // =========================================================================

    @Nested
    @DisplayName("Sequências repetidas (rejeitadas pela Receita Federal)")
    class SequenciasRepetidas {

        @ParameterizedTest(name = "dígito {0} repetido — CPF")
        @DisplayName("Todas as sequências repetidas de CPF devem ser rejeitadas")
        @ValueSource(strings = {
                "00000000000", "11111111111", "22222222222", "33333333333",
                "44444444444", "55555555555", "66666666666", "77777777777",
                "88888888888", "99999999999"
        })
        void cpfSequenciaRepetida(String cpf) {
            assertThat(validator.isValid(cpf, null)).isFalse();
        }

        @ParameterizedTest(name = "dígito {0} repetido — CNPJ")
        @DisplayName("Todas as sequências repetidas de CNPJ devem ser rejeitadas")
        @ValueSource(strings = {
                "00000000000000", "11111111111111", "22222222222222",
                "33333333333333", "44444444444444", "55555555555555",
                "66666666666666", "77777777777777", "88888888888888",
                "99999999999999"
        })
        void cnpjSequenciaRepetida(String cnpj) {
            assertThat(validator.isValid(cnpj, null)).isFalse();
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    @DisplayName("Edge cases — entradas inválidas")
    class EdgeCases {

        @Test
        @DisplayName("null deve ser rejeitado")
        void nulo_rejeitado() {
            assertThat(validator.isValid(null, null)).isFalse();
        }

        @Test
        @DisplayName("String vazia deve ser rejeitada")
        void vazio_rejeitado() {
            assertThat(validator.isValid("", null)).isFalse();
        }

        @Test
        @DisplayName("String em branco deve ser rejeitada")
        void branco_rejeitado() {
            assertThat(validator.isValid("   ", null)).isFalse();
        }

        @Test
        @DisplayName("Apenas letras deve ser rejeitado")
        void apenasLetras_rejeitado() {
            assertThat(validator.isValid("ABCDEFGHIJK", null)).isFalse();
        }

        @Test
        @DisplayName("Mistura de letras e números deve ser rejeitada")
        void misturaLetrasNumeros_rejeitada() {
            assertThat(validator.isValid("529.ABC.247-25", null)).isFalse();
        }
    }
}