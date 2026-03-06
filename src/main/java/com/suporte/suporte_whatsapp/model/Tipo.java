package com.suporte.suporte_whatsapp.model;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "tipo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tipo {

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
}