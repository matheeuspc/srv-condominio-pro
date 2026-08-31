package com.mcardoso.srvcondominiopro.modules.avisos;

import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvisoSegmentacaoTest {

    @Test
    void avisoSemSegmentacaoVisivelParaTodosOsPapeis() {
        assertTrue(AvisoService.visivelParaMorador(null, Role.PROPRIETARIO, true));
        assertTrue(AvisoService.visivelParaMorador(null, Role.INQUILINO, true));
    }

    @Test
    void avisoSegmentadoSoApareceParaOSegmentoCorrespondente() {
        assertTrue(AvisoService.visivelParaMorador(Role.PROPRIETARIO, Role.PROPRIETARIO, true));
        assertFalse(AvisoService.visivelParaMorador(Role.PROPRIETARIO, Role.INQUILINO, true));

        assertTrue(AvisoService.visivelParaMorador(Role.INQUILINO, Role.INQUILINO, true));
        assertFalse(AvisoService.visivelParaMorador(Role.INQUILINO, Role.PROPRIETARIO, true));
    }

    @Test
    void avisoNaoPublicadoNuncaEhVisivelParaMorador() {
        assertFalse(AvisoService.visivelParaMorador(null, Role.PROPRIETARIO, false));
        assertFalse(AvisoService.visivelParaMorador(Role.PROPRIETARIO, Role.PROPRIETARIO, false));
    }
}
