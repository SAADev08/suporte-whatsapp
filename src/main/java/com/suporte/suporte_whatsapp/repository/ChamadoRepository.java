package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Chamado;
import com.suporte.suporte_whatsapp.model.enums.ChamadoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ChamadoRepository extends JpaRepository<Chamado, UUID>,
    JpaSpecificationExecutor<Chamado> {

  List<Chamado> findByStatusAtual(ChamadoStatus status);

  List<Chamado> findByContatoId(UUID contatoId);

  @Query("""
      SELECT ch FROM Chamado ch
      WHERE ch.contato.id = :contatoId
        AND ch.statusAtual <> com.suporte.suporte_whatsapp.model.enums.ChamadoStatus.ENCERRADO
      ORDER BY
            CASE ch.statusAtual
                WHEN com.suporte.suporte_whatsapp.model.enums.ChamadoStatus.AGUARDANDO_CLIENTE THEN 1
                WHEN com.suporte.suporte_whatsapp.model.enums.ChamadoStatus.EM_ATENDIMENTO     THEN 2
                WHEN com.suporte.suporte_whatsapp.model.enums.ChamadoStatus.AGUARDANDO         THEN 3
                ELSE 4
            END ASC,
            ch.dtAbertura DESC
        """)
  List<Chamado> findAbertosParaContato(@Param("contatoId") UUID contatoId);
}