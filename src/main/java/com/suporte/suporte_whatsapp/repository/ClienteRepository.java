package com.suporte.suporte_whatsapp.repository;

import com.suporte.suporte_whatsapp.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.*;

public interface ClienteRepository extends JpaRepository<Cliente, UUID>,
    JpaSpecificationExecutor<Cliente> {

  boolean existsByCpfCnpj(String cpfCnpj);

  boolean existsByCpfCnpjAndIdNot(String cpfCnpj, UUID id);
}