package com.mcardoso.srvcondominiopro.modules.moradores;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoradorUnidadeRepository extends JpaRepository<MoradorUnidade, Long> {

    List<MoradorUnidade> findByUsuarioId(Long usuarioId);

    List<MoradorUnidade> findByUsuarioIdAndStatus(Long usuarioId, StatusMorador status);

    boolean existsByUsuarioIdAndUnidadeId(Long usuarioId, Long unidadeId);

    boolean existsByUnidadeIdAndStatus(Long unidadeId, StatusMorador status);
}
