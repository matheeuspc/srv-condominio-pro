package com.mcardoso.srvcondominiopro.modules.auth;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthTokenExpiracaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 31, 12, 0);

    @Test
    void tokenSemExpiracaoEhInvalido() {
        assertFalse(AuthService.tokenValido(null, AGORA));
    }

    @Test
    void tokenComExpiracaoNoPassadoEhInvalido() {
        assertFalse(AuthService.tokenValido(AGORA.minusSeconds(1), AGORA));
    }

    @Test
    void tokenComExpiracaoNoFuturoEhValido() {
        assertTrue(AuthService.tokenValido(AGORA.plusHours(1), AGORA));
    }
}
