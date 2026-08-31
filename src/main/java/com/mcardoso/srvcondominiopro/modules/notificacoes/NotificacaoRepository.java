package com.mcardoso.srvcondominiopro.modules.notificacoes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuarioCondominioIdOrderByCreatedAtDesc(Long condominioId);

    List<Notificacao> findByUsuarioCondominioIdAndTipoOrderByCreatedAtDesc(
            Long condominioId, TipoNotificacao tipo);

    List<Notificacao> findByUsuarioCondominioIdAndStatusOrderByCreatedAtDesc(
            Long condominioId, StatusNotificacao status);

    List<Notificacao> findByUsuarioCondominioIdAndTipoAndStatusOrderByCreatedAtDesc(
            Long condominioId, TipoNotificacao tipo, StatusNotificacao status);
}
