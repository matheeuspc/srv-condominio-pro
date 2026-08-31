package com.mcardoso.srvcondominiopro.modules.pagamentos;

import com.mcardoso.srvcondominiopro.modules.reservas.Reserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Tabela nova (como reservas): criada pelo Hibernate (ddl-auto=update), sem entrada no schema.sql.
// Relação 1:1 com reservas — uma reserva tem no máximo um pagamento (reserva_id UNIQUE).
@Entity
@Table(name = "pagamentos")
@Getter
@Setter
@NoArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.AGUARDANDO;

    @Column(name = "metodo_pagamento", nullable = false, length = 20)
    private String metodoPagamento = "PIX";

    @Column(name = "mp_payment_id", unique = true)
    private String mpPaymentId;

    @Column(name = "mp_qr_code", columnDefinition = "TEXT")
    private String mpQrCode;

    @Column(name = "mp_qr_code_base64", columnDefinition = "TEXT")
    private String mpQrCodeBase64;

    @Column(name = "mp_ticket_url", length = 500)
    private String mpTicketUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

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
