package com.mcardoso.srvcondominiopro.modules.condominios;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominioRepository extends JpaRepository<Condominio, Long> {

    boolean existsByCnpj(String cnpj);
}
