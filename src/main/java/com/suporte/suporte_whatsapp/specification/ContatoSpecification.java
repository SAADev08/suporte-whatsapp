package com.suporte.suporte_whatsapp.specification;

import com.suporte.suporte_whatsapp.model.Contato;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ContatoSpecification {

    private ContatoSpecification() {
    }

    public static Specification<Contato> comFiltros(String nome, String telefone, String email) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Apenas contatos ativos
            predicates.add(cb.isTrue(root.get("ativo")));

            if (nome != null && !nome.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nome")),
                        "%" + nome.toLowerCase() + "%"));
            }

            if (telefone != null && !telefone.isBlank()) {
                predicates.add(cb.like(root.get("telefone"),
                        "%" + telefone + "%"));
            }

            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}