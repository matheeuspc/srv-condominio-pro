package com.mcardoso.srvcondominiopro.modules.notificacoes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferenciaNotificacaoRepository extends JpaRepository<PreferenciaNotificacao, Long> {

    Optional<PreferenciaNotificacao> findByUsuarioId(Long usuarioId);
}
