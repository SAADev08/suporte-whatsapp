package com.suporte.suporte_whatsapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String nome;

    private String cidade;

    @Column(name = "cpf_cnpj", unique = true, nullable = false)
    private String cpfCnpj;

    @Column(name = "contato_principal")
    private String contatoPrincipal;

    @Column(name = "comercial_responsavel")
    private String comercialResponsavel;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "clientes", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Contato> contatos = new ArrayList<>();
}