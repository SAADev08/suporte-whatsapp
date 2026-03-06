package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
        Optional<Usuario> findByEmail(String email);

        boolean existsByEmail(String email);

        boolean existsByEmailAndIdNot(String email, UUID id);

        @Query(value = """
                        SELECT * FROM usuario u
                        WHERE (:nome IS NULL OR LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
                          AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
                          AND (:perfil IS NULL OR CAST(u.perfil AS VARCHAR) = :perfil)
                        """, nativeQuery = true)
        List<Usuario> findAllComFiltro(
                        @Param("nome") String nome,
                        @Param("email") String email,
                        @Param("perfil") String perfil);
}