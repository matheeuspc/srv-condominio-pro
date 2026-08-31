package com.mcardoso.srvcondominiopro.modules.avisos;

import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    List<Aviso> findByCondominioIdOrderByCreatedAtDesc(Long condominioId);

    // Avisos que um morador pode ver: publicados e sem segmentação ou segmentados para o seu papel.
    @Query("""
            select a from Aviso a
            where a.condominio.id = :condominioId
              and a.publicado = true
              and (a.destinatario is null or a.destinatario = :role)
            order by a.createdAt desc
            """)
    List<Aviso> findVisiveisParaMorador(@Param("condominioId") Long condominioId, @Param("role") Role role);
}
