package com.suporte.suporte_whatsapp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementação da validação de CPF e CNPJ.
 *
 * ── Fluxo de validação ───────────────────────────────────────────────────────
 *
 * 1. Rejeita null e em branco
 * 2. Remove máscara (pontos, traços, barras, espaços)
 * 3. Determina o tipo pelo comprimento: 11 = CPF, 14 = CNPJ
 * 4. Rejeita sequências repetidas ("00000000000", "11111111111111", etc.)
 * 5. Calcula e verifica os dígitos verificadores pelo algoritmo oficial
 *
 * ── Algoritmo CPF (Módulo 11) ────────────────────────────────────────────────
 *
 * Primeiro dígito: multiplica os 9 primeiros dígitos por 10..2,
 * soma, multiplica por 10, toma o módulo 11. Se resultado >= 10 → 0.
 *
 * Segundo dígito: multiplica os 10 primeiros dígitos por 11..2,
 * soma, multiplica por 10, toma o módulo 11. Se resultado >= 10 → 0.
 *
 * ── Algoritmo CNPJ (Módulo 11 com pesos cíclicos) ───────────────────────────
 *
 * Primeiro dígito: pesos [5,4,3,2,9,8,7,6,5,4,3,2] sobre os 12 primeiros.
 * Segundo dígito: pesos [6,5,4,3,2,9,8,7,6,5,4,3,2] sobre os 13 primeiros.
 * Em ambos: resto = soma % 11; dígito = (resto < 2) ? 0 : (11 - resto).
 *
 * Referência: Receita Federal do Brasil.
 */
public class CpfCnpjValidator implements ConstraintValidator<CpfCnpj, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        if (valor == null || valor.isBlank())
            return false;

        String digitos = apenasDigitos(valor);

        return switch (digitos.length()) {
            case 11 -> cpfValido(digitos);
            case 14 -> cnpjValido(digitos);
            default -> false;
        };
    }

    // =========================================================================
    // CPF
    // =========================================================================

    private boolean cpfValido(String cpf) {
        if (sequenciaRepetida(cpf))
            return false;

        int d1 = digitoCpf(cpf, 9, 10);
        int d2 = digitoCpf(cpf, 10, 11);

        return cpf.charAt(9) == ('0' + d1)
                && cpf.charAt(10) == ('0' + d2);
    }

    /**
     * Calcula um dígito verificador do CPF.
     *
     * @param cpf  string com 11 dígitos
     * @param n    quantidade de dígitos a usar no cálculo
     * @param peso peso inicial (decresce até 2)
     */
    private int digitoCpf(String cpf, int n, int peso) {
        int soma = 0;
        for (int i = 0; i < n; i++) {
            soma += (cpf.charAt(i) - '0') * (peso - i);
        }
        int resto = (soma * 10) % 11;
        return (resto == 10 || resto == 11) ? 0 : resto;
    }

    // =========================================================================
    // CNPJ
    // =========================================================================

    private boolean cnpjValido(String cnpj) {
        if (sequenciaRepetida(cnpj))
            return false;

        int d1 = digitoCnpj(cnpj, new int[] { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 });
        int d2 = digitoCnpj(cnpj, new int[] { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 });

        return cnpj.charAt(12) == ('0' + d1)
                && cnpj.charAt(13) == ('0' + d2);
    }

    /**
     * Calcula um dígito verificador do CNPJ.
     *
     * @param cnpj  string com 14 dígitos
     * @param pesos array de pesos — comprimento define quantos dígitos usar
     */
    private int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return (resto < 2) ? 0 : (11 - resto);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Remove qualquer caractere não numérico (máscara, espaços). */
    private String apenasDigitos(String valor) {
        return valor.replaceAll("[^0-9]", "");
    }

    /**
     * Rejeita sequências do tipo "00000000000" ou "11111111111111".
     * São matematicamente válidas pelo algoritmo mas não representam
     * documentos reais — a Receita Federal as rejeita.
     */
    private boolean sequenciaRepetida(String digitos) {
        char primeiro = digitos.charAt(0);
        for (int i = 1; i < digitos.length(); i++) {
            if (digitos.charAt(i) != primeiro)
                return false;
        }
        return true;
    }
}