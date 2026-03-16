package com.suporte.suporte_whatsapp.specification;

import com.suporte.suporte_whatsapp.model.Cliente;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClienteSpecification {

    private ClienteSpecification() {
    }

    public static Specification<Cliente> comFiltros(String nome, String cidade, String cpfCnpj) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Apenas clientes ativos
            predicates.add(cb.isTrue(root.get("ativo")));

            if (nome != null && !nome.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nome")),
                        "%" + nome.toLowerCase() + "%"));
            }

            if (cidade != null && !cidade.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("cidade")),
                        "%" + cidade.toLowerCase() + "%"));
            }

            if (cpfCnpj != null && !cpfCnpj.isBlank()) {
                predicates.add(cb.equal(root.get("cpfCnpj"), cpfCnpj));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
