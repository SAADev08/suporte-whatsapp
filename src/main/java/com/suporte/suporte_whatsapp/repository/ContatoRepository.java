package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ContatoRepository extends JpaRepository<Contato, UUID> {
        boolean existsByTelefone(String telefone);

        boolean existsByTelefoneAndIdNot(String telefone, UUID id);

        Optional<Contato> findByTelefone(String telefone);

        @Query(value = """
                        SELECT * FROM contato c
                        WHERE c.ativo = true
                          AND (:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
                          AND (:telefone IS NULL OR c.telefone LIKE CONCAT('%', :telefone, '%'))
                          AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
                        """, nativeQuery = true)
        List<Contato> findAllAtivosComFiltro(
                        @Param("nome") String nome,
                        @Param("telefone") String telefone,
                        @Param("email") String email);
}
