package com.suporte.suporte_whatsapp.service;

import com.suporte.suporte_whatsapp.dto.*;
import com.suporte.suporte_whatsapp.model.*;
import com.suporte.suporte_whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TipoService {

    private final TipoRepository tipoRepository;
    private final SubtipoRepository subtipoRepository;

    public List<TipoResponse> listarAtivos() {
        return tipoRepository.findByAtivoTrue().stream().map(TipoResponse::from).toList();
    }

    @Transactional
    public TipoResponse criar(TipoRequest req) {
        return TipoResponse.from(tipoRepository.save(Tipo.builder().nome(req.getNome()).build()));
    }

    public List<SubtipoResponse> listarSubtipos(UUID tipoId) {
        return subtipoRepository.findByTipoIdAndAtivoTrue(tipoId)
                .stream().map(SubtipoResponse::from).toList();
    }

    @Transactional
    public SubtipoResponse criarSubtipo(UUID tipoId, SubtipoRequest req) {
        Tipo tipo = tipoRepository.findById(tipoId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo não encontrado: " + tipoId));
        return SubtipoResponse.from(
                subtipoRepository.save(Subtipo.builder().nome(req.getNome()).tipo(tipo).build()));
    }
}
