package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.Usuario;
import com.suporte.suporte_whatsapp.model.enums.Perfil;
import com.suporte.suporte_whatsapp.repository.UsuarioRepository;
import com.suporte.suporte_whatsapp.specification.UsuarioSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UsuarioResponse> listar(String nome, String email, Perfil perfil, Pageable pageable) {
        return usuarioRepository
                .findAll(UsuarioSpecification.comFiltros(nome, email, perfil), pageable)
                .map(UsuarioResponse::from);
    }

    public UsuarioResponse buscarPorId(UUID id) {
        return UsuarioResponse.from(findOrThrow(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest req) {
        if (req.getSenha() == null || req.getSenha().isBlank())
            throw new IllegalArgumentException("Senha obrigatória ao criar usuário");
        if (usuarioRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("E-mail já cadastrado: " + req.getEmail());
        Usuario u = Usuario.builder()
                .nome(req.getNome()).email(req.getEmail())
                .senha(passwordEncoder.encode(req.getSenha()))
                .perfil(req.getPerfil())
                .build();
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioRequest req) {
        Usuario u = findOrThrow(id);
        if (usuarioRepository.existsByEmailAndIdNot(req.getEmail(), id))
            throw new IllegalArgumentException("E-mail já utilizado por outro usuário");
        u.setNome(req.getNome());
        u.setEmail(req.getEmail());
        u.setPerfil(req.getPerfil());
        if (req.getSenha() != null && !req.getSenha().isBlank())
            u.setSenha(passwordEncoder.encode(req.getSenha()));
        return UsuarioResponse.from(usuarioRepository.save(u));
    }

    @Transactional
    public void inativar(UUID id) {
        Usuario u = findOrThrow(id);
        u.setAtivo(false);
        usuarioRepository.save(u);
    }

    @Transactional
    public void reativar(UUID id) {
        Usuario u = findOrThrow(id);
        u.setAtivo(true);
        usuarioRepository.save(u);
    }

    public Usuario findOrThrow(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));
    }

    public Usuario findByEmailOrThrow(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + email));
    }
}