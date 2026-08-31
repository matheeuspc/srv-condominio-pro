package com.mcardoso.srvcondominiopro.modules.unidades;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    List<Unidade> findByCondominioIdOrderByBlocoAscNumeroAsc(Long condominioId);

    boolean existsByCondominioIdAndBlocoAndNumero(Long condominioId, String bloco, String numero);
}
