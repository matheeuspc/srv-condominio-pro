package com.mcardoso.srvcondominiopro.modules.reservas;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservaServiceOverlapTest {

    private static boolean sobrepoe(String aInicio, String aFim, String bInicio, String bFim) {
        return ReservaService.intervalosSobrepoem(
                LocalTime.parse(aInicio), LocalTime.parse(aFim),
                LocalTime.parse(bInicio), LocalTime.parse(bFim));
    }

    @Test
    void intervalosDisjuntosNaoSobrepoem() {
        assertFalse(sobrepoe("08:00", "10:00", "14:00", "16:00"));
        assertFalse(sobrepoe("14:00", "16:00", "08:00", "10:00"));
    }

    @Test
    void bordasQueApenasSeTocamNaoSobrepoem() {
        assertFalse(sobrepoe("08:00", "09:00", "09:00", "10:00"));
        assertFalse(sobrepoe("09:00", "10:00", "08:00", "09:00"));
    }

    @Test
    void intervalosComInterseccaoParcialSobrepoem() {
        assertTrue(sobrepoe("08:00", "10:00", "09:00", "11:00"));
        assertTrue(sobrepoe("09:00", "11:00", "08:00", "10:00"));
    }

    @Test
    void intervaloContidoSobrepoe() {
        assertTrue(sobrepoe("08:00", "12:00", "09:00", "10:00"));
        assertTrue(sobrepoe("09:00", "10:00", "08:00", "12:00"));
    }

    @Test
    void intervalosIdenticosSobrepoem() {
        assertTrue(sobrepoe("08:00", "09:00", "08:00", "09:00"));
    }
}
