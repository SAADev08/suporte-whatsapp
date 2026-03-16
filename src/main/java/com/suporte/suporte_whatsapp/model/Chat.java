package com.suporte.suporte_whatsapp.model;

import com.suporte.suporte_whatsapp.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "message_id")
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chamado")
    private Chamado chamado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contato", nullable = false)
    private Contato contato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatOrigem origem;

    @Column(name = "dt_envio", nullable = false)
    private LocalDateTime dtEnvio;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column(name = "file_url")
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_midia", nullable = false)
    private TipoMidia tipoMidia;

    @Column(name = "fone_cliente", nullable = false)
    private String foneCliente;

    @Column(name = "fone_suporte")
    private String foneSuporte;

    @Column(name = "nome_grupo")
    private String nomeGrupo;

    @Column(name = "nome_contato", nullable = false)
    private String nomeContato;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}