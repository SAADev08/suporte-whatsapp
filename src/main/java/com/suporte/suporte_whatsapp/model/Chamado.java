package com.suporte.suporte_whatsapp.model;

import com.suporte.suporte_whatsapp.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chamado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chamado {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Column(columnDefinition = "TEXT")
    private String solucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_atual", nullable = false, columnDefinition = "chamado_status_enum")
    private ChamadoStatus statusAtual;

    @Column(name = "dt_abertura", nullable = false)
    private LocalDateTime dtAbertura;

    @Column(name = "dt_encerramento")
    private LocalDateTime dtEncerramento;

    @Column(name = "dt_primeira_mensagem")
    private LocalDateTime dtPrimeiraMensagem;

    @Column(name = "dt_primeira_resposta")
    private LocalDateTime dtPrimeiraResposta;

    @Column(name = "tempo_total_segundos")
    private Long tempoTotalSegundos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "origem_enum")
    private Origem origem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contato", nullable = false)
    private Contato contato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_responsavel")
    private Usuario usuarioResponsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_subtipo")
    private Subtipo subtipo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}