package com.mcardoso.srvcondominiopro.modules.areas;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaComumRepository extends JpaRepository<AreaComum, Long> {

    List<AreaComum> findByCondominioIdOrderByNomeAsc(Long condominioId);
}
