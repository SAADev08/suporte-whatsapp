package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Chat;
import com.suporte.suporte_whatsapp.model.enums.ChatOrigem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
  List<Chat> findByChamadoIdOrderByDtEnvioAsc(UUID chamadoId);

  List<Chat> findByChamadoIsNullOrderByDtEnvioAsc();

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
}