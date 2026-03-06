package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.*;
import com.suporte.suporte_whatsapp.model.enums.ChamadoStatus;
import com.suporte.suporte_whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ContatoService {

    private final ContatoRepository contatoRepository;
    private final ClienteRepository clienteRepository;
    private final ChamadoRepository chamadoRepository;

    public List<ContatoResponse> listar(String nome, String telefone, String email) {
        return contatoRepository.findAllAtivosComFiltro(nome, telefone, email)
                .stream().map(ContatoResponse::from).toList();
    }

    public ContatoResponse buscarPorId(UUID id) {
        return ContatoResponse.from(findOrThrow(id));
    }

    @Transactional
    public ContatoResponse criar(ContatoRequest req) {
        if (contatoRepository.existsByTelefone(req.getTelefone()))
            throw new IllegalArgumentException("Telefone já cadastrado: " + req.getTelefone());

        Contato c = Contato.builder()
                .nome(req.getNome()).telefone(req.getTelefone()).email(req.getEmail())
                .clientes(resolverClientes(req.getClienteIds()))
                .build();
        return ContatoResponse.from(contatoRepository.save(c));
    }

    @Transactional
    public ContatoResponse atualizar(UUID id, ContatoRequest req) {
        Contato c = findOrThrow(id);
        if (contatoRepository.existsByTelefoneAndIdNot(req.getTelefone(), id))
            throw new IllegalArgumentException("Telefone já utilizado por outro contato");
        if (req.getClienteIds() == null || req.getClienteIds().isEmpty())
            throw new IllegalArgumentException("Contato deve estar vinculado a pelo menos um cliente");
        c.setNome(req.getNome());
        c.setTelefone(req.getTelefone());
        c.setEmail(req.getEmail());
        c.setClientes(resolverClientes(req.getClienteIds()));
        return ContatoResponse.from(contatoRepository.save(c));
    }

    @Transactional
    public void inativar(UUID id) {
        Contato c = findOrThrow(id);
        boolean temAbertos = chamadoRepository.findByContatoId(id).stream()
                .anyMatch(ch -> ch.getStatusAtual() != ChamadoStatus.ENCERRADO);
        if (temAbertos)
            throw new IllegalStateException("Contato possui chamados abertos");
        c.setAtivo(false);
        contatoRepository.save(c);
    }

    public Contato findOrThrow(UUID id) {
        return contatoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contato não encontrado: " + id));
    }

    private List<Cliente> resolverClientes(List<UUID> ids) {
        List<Cliente> clientes = clienteRepository.findAllById(ids);
        if (clientes.size() != ids.size())
            throw new IllegalArgumentException("Um ou mais clientes não encontrados");
        return clientes;
    }
}
