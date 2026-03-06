package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
        boolean existsByCpfCnpj(String cpfCnpj);

        boolean existsByCpfCnpjAndIdNot(String cpfCnpj, UUID id);

        @Query(value = """
                        SELECT * FROM cliente c
                        WHERE c.ativo = true
                          AND (:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
                          AND (:cidade IS NULL OR LOWER(c.cidade) LIKE LOWER(CONCAT('%', :cidade, '%')))
                          AND (:cpfCnpj IS NULL OR c.cpf_cnpj = :cpfCnpj)
                        """, nativeQuery = true)
        List<Cliente> findAllAtivosComFiltro(
                        @Param("nome") String nome,
                        @Param("cidade") String cidade,
                        @Param("cpfCnpj") String cpfCnpj);
}