package com.mcardoso.srvcondominiopro.modules.reservas;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioIdOrderByDataDescHoraInicioDesc(Long usuarioId);

    List<Reserva> findByAreaIdOrderByDataDescHoraInicioDesc(Long areaId);

    List<Reserva> findByAreaCondominioIdOrderByDataDescHoraInicioDesc(Long condominioId);

    List<Reserva> findByAreaIdAndDataAndStatusIn(Long areaId, LocalDate data, Collection<StatusReserva> status);

    long countByUsuarioIdAndAreaIdAndDataBetweenAndStatusIn(
            Long usuarioId, Long areaId, LocalDate inicio, LocalDate fim, Collection<StatusReserva> status);

    boolean existsByAreaIdAndStatusIn(Long areaId, Collection<StatusReserva> status);
}
