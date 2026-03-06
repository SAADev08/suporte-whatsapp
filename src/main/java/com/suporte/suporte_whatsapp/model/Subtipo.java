package com.suporte.suporte_whatsapp.model;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "subtipo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtipo {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo", nullable = false)
    private Tipo tipo;
}