package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.Cliente;
import com.suporte.suporte_whatsapp.model.enums.ChamadoStatus;
import com.suporte.suporte_whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ChamadoRepository chamadoRepository;

    public List<ClienteResponse> listar(String nome, String cidade, String cpfCnpj) {
        return clienteRepository.findAllAtivosComFiltro(nome, cidade, cpfCnpj)
                .stream().map(ClienteResponse::from).toList();
    }

    public ClienteResponse buscarPorId(UUID id) {
        return ClienteResponse.from(findOrThrow(id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest req) {
        if (clienteRepository.existsByCpfCnpj(req.getCpfCnpj()))
            throw new IllegalArgumentException("CPF/CNPJ já cadastrado: " + req.getCpfCnpj());

        Cliente c = Cliente.builder()
                .nome(req.getNome()).cidade(req.getCidade()).cpfCnpj(req.getCpfCnpj())
                .contatoPrincipal(req.getContatoPrincipal())
                .comercialResponsavel(req.getComercialResponsavel())
                .build();
        return ClienteResponse.from(clienteRepository.save(c));
    }

    @Transactional
    public ClienteResponse atualizar(UUID id, ClienteRequest req) {
        Cliente c = findOrThrow(id);
        if (clienteRepository.existsByCpfCnpjAndIdNot(req.getCpfCnpj(), id))
            throw new IllegalArgumentException("CPF/CNPJ já utilizado por outro cliente");
        c.setNome(req.getNome());
        c.setCidade(req.getCidade());
        c.setCpfCnpj(req.getCpfCnpj());
        c.setContatoPrincipal(req.getContatoPrincipal());
        c.setComercialResponsavel(req.getComercialResponsavel());
        return ClienteResponse.from(clienteRepository.save(c));
    }

    @Transactional
    public void inativar(UUID id) {
        Cliente c = findOrThrow(id);
        boolean temAbertos = chamadoRepository.findByContatoId(id).stream()
                .anyMatch(ch -> ch.getStatusAtual() != ChamadoStatus.ENCERRADO);
        if (temAbertos)
            throw new IllegalStateException("Cliente possui chamados abertos. Encerre-os antes de inativar.");
        c.setAtivo(false);
        clienteRepository.save(c);
    }

    public Cliente findOrThrow(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + id));
    }
}
