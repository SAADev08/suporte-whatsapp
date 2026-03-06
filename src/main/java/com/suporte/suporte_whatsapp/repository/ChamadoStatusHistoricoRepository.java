package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.ChamadoStatusHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ChamadoStatusHistoricoRepository extends JpaRepository<ChamadoStatusHistorico, UUID> {
    List<ChamadoStatusHistorico> findByChamadoIdOrderByDtInicioAsc(UUID chamadoId);

    Optional<ChamadoStatusHistorico> findByChamadoIdAndDtFimIsNull(UUID chamadoId);
}
