package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.*;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>,
    JpaSpecificationExecutor<Usuario> {

  Optional<Usuario> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, UUID id);
}