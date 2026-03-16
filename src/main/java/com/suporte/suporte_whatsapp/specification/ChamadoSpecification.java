package com.suporte.suporte_whatsapp.specification;

import com.suporte.suporte_whatsapp.model.Chamado;
import com.suporte.suporte_whatsapp.model.enums.ChamadoStatus;
import com.suporte.suporte_whatsapp.model.enums.Origem;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChamadoSpecification {

    private ChamadoSpecification() {
    }

    public static Specification<Chamado> comFiltros(ChamadoStatus status, Origem origem,
            UUID contatoId, UUID usuarioId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("statusAtual").as(String.class), status.name()));
            }

            if (origem != null) {
                predicates.add(cb.equal(root.get("origem").as(String.class), origem.name()));
            }

            if (contatoId != null) {
                predicates.add(cb.equal(root.get("contato").get("id"), contatoId));
            }

            if (usuarioId != null) {
                predicates.add(cb.equal(root.get("usuarioResponsavel").get("id"), usuarioId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}