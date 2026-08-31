package com.mcardoso.srvcondominiopro.modules.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Usuario> findByTokenConvite(String tokenConvite);

    long countByCondominioIdAndAtivoTrue(Long condominioId);

    long countByCondominioIdAndAtivoTrueAndRoleIn(Long condominioId, Collection<Role> roles);

    List<Usuario> findByCondominioIdAndRoleInOrderByNomeAsc(Long condominioId, Collection<Role> roles);
}
