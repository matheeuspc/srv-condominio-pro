package com.mcardoso.srvcondominiopro.modules.moradores;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoradorUnidadeRepository extends JpaRepository<MoradorUnidade, Long> {

    List<MoradorUnidade> findByUsuarioId(Long usuarioId);

    List<MoradorUnidade> findByUsuarioIdAndStatus(Long usuarioId, StatusMorador status);

    List<MoradorUnidade> findByUnidadeCondominioIdAndStatus(Long condominioId, StatusMorador status);

    boolean existsByUsuarioIdAndUnidadeId(Long usuarioId, Long unidadeId);

    boolean existsByUsuarioIdAndUnidadeIdAndStatus(Long usuarioId, Long unidadeId, StatusMorador status);

    boolean existsByUnidadeIdAndStatus(Long unidadeId, StatusMorador status);
}
