package com.mcardoso.srvcondominiopro.modules.faqs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findByCondominioIdOrderByOrdemAscIdAsc(Long condominioId);

    List<Faq> findByCondominioIdAndAtivaTrueOrderByOrdemAscIdAsc(Long condominioId);

    List<Faq> findByCondominioIdAndCategoriaOrderByOrdemAscIdAsc(Long condominioId, CategoriaFaq categoria);

    List<Faq> findByCondominioIdAndAtivaTrueAndCategoriaOrderByOrdemAscIdAsc(Long condominioId, CategoriaFaq categoria);

    // Busca por palavra-chave em pergunta ou resposta (CONTEXT 5.4). Filtro de `ativa` fica no service.
    @Query("""
            select f from Faq f
            where f.condominio.id = :condominioId
              and (lower(f.pergunta) like lower(concat('%', :termo, '%'))
                   or lower(f.resposta) like lower(concat('%', :termo, '%')))
            order by f.ordem asc, f.id asc
            """)
    List<Faq> buscar(@Param("condominioId") Long condominioId, @Param("termo") String termo);
}
