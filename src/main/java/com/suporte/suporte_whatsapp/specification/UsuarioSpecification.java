package com.suporte.suporte_whatsapp.specification;

import com.suporte.suporte_whatsapp.model.Usuario;
import com.suporte.suporte_whatsapp.model.enums.Perfil;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> comFiltros(String nome, String email, Perfil perfil) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (nome != null && !nome.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nome")),
                        "%" + nome.toLowerCase() + "%"));
            }

            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"));
            }

            if (perfil != null) {
                predicates.add(cb.equal(root.get("perfil").as(String.class), perfil.name()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}