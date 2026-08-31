package com.mcardoso.srvcondominiopro.modules.notificacoes;

import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Preferência de notificação por morador (CONTEXT 5.5 / 6.10). Tabela nova (não consta no CONTEXT 7),
// criada pelo Hibernate (ddl-auto=update). Ausência de linha => usa os defaults (email on, whatsapp off).
@Entity
@Table(
        name = "preferencias_notificacao",
        uniqueConstraints = @UniqueConstraint(columnNames = "usuario_id")
)
@Getter
@Setter
@NoArgsConstructor
public class PreferenciaNotificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notificar_email", nullable = false)
    private boolean notificarEmail = true;

    @Column(name = "notificar_whatsapp", nullable = false)
    private boolean notificarWhatsapp = false;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
