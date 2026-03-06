package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Subtipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SubtipoRepository extends JpaRepository<Subtipo, UUID> {
    List<Subtipo> findByTipoId(UUID tipoId);

    List<Subtipo> findByTipoIdAndAtivoTrue(UUID tipoId);
}
