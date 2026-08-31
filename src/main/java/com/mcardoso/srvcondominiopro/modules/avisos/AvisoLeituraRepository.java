package com.mcardoso.srvcondominiopro.modules.avisos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvisoLeituraRepository extends JpaRepository<AvisoLeitura, Long> {

    boolean existsByAvisoIdAndUsuarioId(Long avisoId, Long usuarioId);

    long countByAvisoId(Long avisoId);

    long deleteByAvisoId(Long avisoId);

    @Query("select l.aviso.id from AvisoLeitura l where l.usuarioId = :usuarioId")
    List<Long> findAvisoIdsLidosPorUsuario(@Param("usuarioId") Long usuarioId);
}
