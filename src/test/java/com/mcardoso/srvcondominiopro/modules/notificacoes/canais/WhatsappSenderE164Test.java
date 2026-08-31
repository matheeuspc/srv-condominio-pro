package com.mcardoso.srvcondominiopro.modules.notificacoes.canais;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WhatsappSenderE164Test {

    @Test
    void mantemNumeroJaEmFormatoInternacional() {
        assertEquals("+5511988887777", WhatsappSender.normalizarE164("+55 (11) 98888-7777"));
    }

    @Test
    void prefixaMaisQuandoJaComeçaCom55() {
        assertEquals("+5511988887777", WhatsappSender.normalizarE164("5511988887777"));
        assertEquals("+551133334444", WhatsappSender.normalizarE164("55 11 3333-4444"));
    }

    @Test
    void assumeBrasilQuandoVemSoODddEoNumero() {
        assertEquals("+5511988887777", WhatsappSender.normalizarE164("(11) 98888-7777"));
    }
}
