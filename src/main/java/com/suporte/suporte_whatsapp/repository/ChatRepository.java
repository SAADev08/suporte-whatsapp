package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.dto.FilaAgrupadaResponse;
import com.suporte.suporte_whatsapp.model.Chat;
import com.suporte.suporte_whatsapp.model.enums.ChatOrigem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

  // Paginado — usado pelo ChatService (porChamado e fila)
  Page<Chat> findByChamadoIdOrderByDtEnvioAsc(UUID chamadoId, Pageable pageable);

  Page<Chat> findByChamadoIsNullOrderByDtEnvioAsc(Pageable pageable);

  // Sem paginação — mantidos para uso interno (WebhookService, etc.)
  List<Chat> findByFoneClienteAndChamadoIsNull(String foneCliente);

  boolean existsByChamadoIdAndOrigem(UUID chamadoId, ChatOrigem origem);

  @Query("""
      SELECT c FROM Chat c
      WHERE c.origem = com.suporte.suporte_whatsapp.model.enums.ChatOrigem.CLIENTE
        AND c.chamado IS NOT NULL
        AND c.dtEnvio < :limite
        AND NOT EXISTS (
            SELECT r FROM Chat r
            WHERE r.chamado.id = c.chamado.id
              AND r.origem = com.suporte.suporte_whatsapp.model.enums.ChatOrigem.SUPORTE
              AND r.dtEnvio > c.dtEnvio
        )
      """)
  List<Chat> findMensagensSemResposta(@Param("limite") LocalDateTime limite);

  // -------------------------------------------------------------------------
  // Fila agrupada por contato — GET /api/chat/fila/agrupada
  // -------------------------------------------------------------------------

  /**
   * Retorna mensagens sem chamado agrupadas por contato, com contagens e
   * timestamps agregados para a tela de triagem do analista.
   *
   * Por que projeção de construtor (new FilaAgrupadaResponse(...)):
   * Carregar entidades Chat completas e agrupar no Java seria inviável
   * com volume real — N chats × M campos × lazy loads. A query agrega
   * no banco e retorna exatamente o que o frontend precisa.
   *
   * Por que não usamos ultimoTexto/ultimoTipoMidia diretamente aqui:
   * JPQL não suporta subquery correlacionada no SELECT de forma portável
   * com todos os dialetos JPA. O preview da última mensagem é resolvido
   * em uma segunda query por lote no ChatService, evitando N+1 por
   * contato e mantendo a query principal simples e paginável.
   *
   * Ordenação: contato com mensagem mais antiga primeiro (maior urgência
   * de SLA), depois por maior volume de mensagens como critério de desempate.
   *
   * countQuery: obrigatório para paginação correta — o Spring Data não
   * consegue derivar o COUNT de queries com GROUP BY automaticamente.
   */
  @Query(value = """
      SELECT new com.suporte.suporte_whatsapp.dto.FilaAgrupadaResponse(
          c.contato.id,
          c.nomeContato,
          c.foneCliente,
          c.contato.pendenteVinculacao,
          COUNT(c.id),
          MIN(c.dtEnvio),
          MAX(c.dtEnvio)
      )
      FROM Chat c
      WHERE c.chamado IS NULL
        AND c.origem = com.suporte.suporte_whatsapp.model.enums.ChatOrigem.CLIENTE
      GROUP BY c.contato.id, c.nomeContato, c.foneCliente, c.contato.pendenteVinculacao
      ORDER BY MIN(c.dtEnvio) ASC, COUNT(c.id) DESC
      """, countQuery = """
      SELECT COUNT(DISTINCT c.contato.id)
      FROM Chat c
      WHERE c.chamado IS NULL
        AND c.origem = com.suporte.suporte_whatsapp.model.enums.ChatOrigem.CLIENTE
      """)
  Page<FilaAgrupadaResponse> findFilaAgrupada(Pageable pageable);

  boolean existsByMessageId(String messageId);

  /**
   * Busca a mensagem mais recente sem chamado de cada contato informado,
   * usada pelo ChatService para preencher o preview (ultimoTexto/ultimoTipoMidia)
   * após a query principal de agrupamento — evita N+1 queries.
   *
   * Retorna List<Object[]> com [contatoId, texto, tipoMidia] por contato.
   */
  @Query("""
      SELECT c.contato.id, c.texto, c.tipoMidia
      FROM Chat c
      WHERE c.chamado IS NULL
        AND c.origem = com.suporte.suporte_whatsapp.model.enums.ChatOrigem.CLIENTE
        AND c.contato.id IN :contatoIds
        AND c.dtEnvio = (
            SELECT MAX(c2.dtEnvio)
            FROM Chat c2
            WHERE c2.contato.id = c.contato.id
              AND c2.chamado IS NULL
              AND c2.origem = com.suporte.suporte_whatsapp.model.enums.ChatOrigem.CLIENTE
        )
      """)
  List<Object[]> findUltimaMensagemPorContatos(@Param("contatoIds") List<UUID> contatoIds);
}