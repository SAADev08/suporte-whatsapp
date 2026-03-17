package com.suporte.suporte_whatsapp.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Valida que o valor anotado é um CPF (11 dígitos) ou CNPJ (14 dígitos)
 * numericamente válido, aceitando tanto o formato com máscara quanto
 * apenas dígitos.
 *
 * Exemplos aceitos:
 * CPF → "529.982.247-25" ou "52998224725"
 * CNPJ → "11.222.333/0001-81" ou "11222333000181"
 *
 * Exemplos rejeitados:
 * - Sequências repetidas ("000.000.000-00", "11.111.111/1111-11")
 * - Dígitos verificadores incorretos
 * - Qualquer outra quantidade de dígitos
 * - null ou em branco
 *
 * Uso:
 * 
 * @CpfCnpj
 *          private String cpfCnpj;
 */
@Documented
@Constraint(validatedBy = CpfCnpjValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface CpfCnpj {

    String message() default "CPF ou CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}