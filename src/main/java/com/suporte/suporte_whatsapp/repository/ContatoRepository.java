package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Contato;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.*;

public interface ContatoRepository extends JpaRepository<Contato, UUID>,
    JpaSpecificationExecutor<Contato> {

  boolean existsByTelefone(String telefone);

  boolean existsByTelefoneAndIdNot(String telefone, UUID id);

  Optional<Contato> findByTelefone(String telefone);

  /**
   * Retorna contatos criados via webhook que ainda não possuem
   * vínculo com nenhum Cliente, ordenados pelo mais recente.
   *
   * Alimenta o endpoint GET /api/contatos/pendentes e é usado
   * pelo ContatoService para reemitir alertas WebSocket na
   * reconexão do frontend.
   */
  Page<Contato> findByPendenteVinculacaoTrueOrderByCreatedAtDesc(Pageable pageable);

}