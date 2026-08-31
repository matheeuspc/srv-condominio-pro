package com.mcardoso.srvcondominiopro.modules.areas;

import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "areas_comuns")
@Getter
@Setter
@NoArgsConstructor
public class AreaComum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private Integer capacidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxa = BigDecimal.ZERO;

    @Column(name = "requer_aprovacao", nullable = false)
    private boolean requerAprovacao = false;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "horario_inicio", nullable = false, length = 5)
    private String horarioInicio;

    @Column(name = "horario_fim", nullable = false, length = 5)
    private String horarioFim;

    @Column(name = "antecedencia_min", nullable = false)
    private Integer antecedenciaMin = 1;

    @Column(name = "antecedencia_max", nullable = false)
    private Integer antecedenciaMax = 30;

    @Column(name = "limite_mensal")
    private Integer limiteMensal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

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
