package com.suporte.suporte_whatsapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "contato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contato {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String telefone;

    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Indica que este contato foi criado automaticamente via webhook (número
     * desconhecido) e ainda não possui vínculo com nenhum Cliente.
     *
     * Enquanto TRUE:
     * - Mensagens continuam sendo salvas normalmente em CHAT.
     * - O analista é alertado via WebSocket (/topic/contatos-pendentes).
     * - A criação de chamados fica bloqueada até a vinculação ser feita.
     *
     * Passa para FALSE quando o analista vincula o contato a pelo menos
     * um Cliente via PUT /api/contatos/{id} (ContatoService.atualizar).
     *
     * DISTINÇÃO: ativo=false significa inativação manual; este campo
     * é exclusivo para o fluxo de triagem de contatos novos via webhook.
     */
    @Column(name = "pendente_vinculacao", nullable = false)
    @Builder.Default
    private Boolean pendenteVinculacao = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "contato_cliente", joinColumns = @JoinColumn(name = "id_contato"), inverseJoinColumns = @JoinColumn(name = "id_cliente"))
    @Builder.Default
    private List<Cliente> clientes = new ArrayList<>();
}