package com.suporte.suporte_whatsapp.model;

import com.suporte.suporte_whatsapp.model.enums.ChamadoStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chamado_status_historico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChamadoStatusHistorico {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chamado", nullable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChamadoStatus status;

    @Column(name = "dt_inicio", nullable = false)
    private LocalDateTime dtInicio;

    @Column(name = "dt_fim")
    private LocalDateTime dtFim;

    @Column(name = "tempo_em_status_segundos")
    private Long tempoEmStatusSegundos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_responsavel")
    private Usuario usuarioResponsavel;
}
