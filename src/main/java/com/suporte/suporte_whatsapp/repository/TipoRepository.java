package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TipoRepository extends JpaRepository<Tipo, UUID> {
    List<Tipo> findByAtivoTrue();
}
