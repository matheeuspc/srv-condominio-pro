package com.mcardoso.srvcondominiopro.modules.relatorios;

import com.mcardoso.srvcondominiopro.modules.relatorios.RelatorioService.Periodo;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelatorioPeriodoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 31);

    @Test
    void semParametrosUsaJanelaDe12MesesAteHoje() {
        Periodo periodo = RelatorioService.resolverPeriodo(null, null, HOJE);
        assertEquals(LocalDate.of(2025, 8, 1), periodo.inicio());
        assertEquals(HOJE, periodo.fim());
    }

    @Test
    void apenasFimAjustaOInicioParaDozeMesesAntes() {
        Periodo periodo = RelatorioService.resolverPeriodo(null, LocalDate.of(2026, 6, 15), HOJE);
        assertEquals(LocalDate.of(2025, 6, 1), periodo.inicio());
        assertEquals(LocalDate.of(2026, 6, 15), periodo.fim());
    }

    @Test
    void respeitaInicioEFimQuandoAmbosInformados() {
        Periodo periodo = RelatorioService.resolverPeriodo(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), HOJE);
        assertEquals(LocalDate.of(2026, 1, 1), periodo.inicio());
        assertEquals(LocalDate.of(2026, 3, 31), periodo.fim());
    }

    @Test
    void inicioDepoisDoFimEhRejeitado() {
        assertThrows(AppException.class, () -> RelatorioService.resolverPeriodo(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1), HOJE));
    }

    @Test
    void chaveMesFormataComZeroAEsquerda() {
        assertEquals("2026-03", RelatorioService.chaveMes(LocalDate.of(2026, 3, 7)));
        assertEquals("2026-11", RelatorioService.chaveMes(LocalDate.of(2026, 11, 30)));
    }
}
