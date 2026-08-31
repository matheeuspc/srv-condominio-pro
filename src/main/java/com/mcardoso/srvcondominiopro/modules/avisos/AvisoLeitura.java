package com.mcardoso.srvcondominiopro.modules.avisos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Registro de que um morador leu um aviso (CONTEXT 5.3). Um por (aviso, usuário).
// CONTEXT: aviso_leituras.usuario_id não tem FK declarada — mantido como coluna simples.
@Entity
@Table(
        name = "aviso_leituras",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aviso_id", "usuario_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class AvisoLeitura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean lido = true;

    @Column(name = "lido_em", nullable = false)
    private LocalDateTime lidoEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aviso_id", nullable = false)
    private Aviso aviso;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @PrePersist
    void onCreate() {
        if (lidoEm == null) {
            lidoEm = LocalDateTime.now();
        }
    }
}
