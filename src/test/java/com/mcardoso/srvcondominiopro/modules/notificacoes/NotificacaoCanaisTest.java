package com.mcardoso.srvcondominiopro.modules.notificacoes;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacaoCanaisTest {

    @Test
    void semNenhumCanalConfiguradoNaoEnviaNada() {
        assertTrue(NotificacaoService.canais(true, true, false, false, true).isEmpty());
    }

    @Test
    void emailSaiQuandoPreferidoEConfigurado() {
        assertEquals(EnumSet.of(TipoNotificacao.EMAIL),
                NotificacaoService.canais(true, false, true, false, true));
    }

    @Test
    void emailDesligadoNaPreferenciaNaoSaiMesmoConfigurado() {
        assertTrue(NotificacaoService.canais(false, false, true, true, true).isEmpty());
    }

    @Test
    void whatsappExigePreferenciaConfiguracaoETelefone() {
        // sem telefone
        assertTrue(NotificacaoService.canais(false, true, true, true, false).isEmpty());
        // sem configuração do provedor
        assertTrue(NotificacaoService.canais(false, true, true, false, true).isEmpty());
        // tudo presente
        assertEquals(EnumSet.of(TipoNotificacao.WHATSAPP),
                NotificacaoService.canais(false, true, true, true, true));
    }

    @Test
    void ambosOsCanaisQuandoTudoHabilitado() {
        assertEquals(EnumSet.of(TipoNotificacao.EMAIL, TipoNotificacao.WHATSAPP),
                NotificacaoService.canais(true, true, true, true, true));
    }
}
