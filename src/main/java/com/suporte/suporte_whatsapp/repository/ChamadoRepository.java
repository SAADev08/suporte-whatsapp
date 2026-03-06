package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Chamado;
import com.suporte.suporte_whatsapp.model.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ChamadoRepository extends JpaRepository<Chamado, UUID> {
  List<Chamado> findByStatusAtual(ChamadoStatus status);

  List<Chamado> findByContatoId(UUID contatoId);

  @Query(value = """
      SELECT * FROM chamado ch
      WHERE (CAST(:status AS VARCHAR) IS NULL OR CAST(ch.status_atual AS VARCHAR) = :status)
        AND (CAST(:origem AS VARCHAR) IS NULL OR CAST(ch.origem AS VARCHAR) = :origem)
        AND (CAST(:contatoId AS VARCHAR) IS NULL OR ch.contato_id = CAST(:contatoId AS UUID))
        AND (CAST(:usuarioId AS VARCHAR) IS NULL OR ch.usuario_responsavel_id = CAST(:usuarioId AS UUID))
      ORDER BY ch.dt_abertura DESC
      """, nativeQuery = true)
  List<Chamado> findAllComFiltro(
      @Param("status") String status,
      @Param("origem") String origem,
      @Param("contatoId") String contatoId,
      @Param("usuarioId") String usuarioId);

  @Query("""
      SELECT ch FROM Chamado ch
      WHERE ch.contato.id = :contatoId
        AND ch.statusAtual <> "ENCERRADO"
      ORDER BY ch.dtAbertura DESC
      """)
  List<Chamado> findAbertosParaContato(@Param("contatoId") UUID contatoId);
}
