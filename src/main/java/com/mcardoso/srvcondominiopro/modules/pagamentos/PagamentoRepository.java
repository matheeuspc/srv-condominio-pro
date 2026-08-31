package com.mcardoso.srvcondominiopro.modules.pagamentos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByReservaId(Long reservaId);

    Optional<Pagamento> findByMpPaymentId(String mpPaymentId);

    List<Pagamento> findByReservaAreaCondominioIdOrderByCreatedAtDesc(Long condominioId);
}
