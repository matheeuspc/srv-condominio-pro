package com.mcardoso.srvcondominiopro.modules.condominios;

import com.mcardoso.srvcondominiopro.modules.condominios.dto.CondominioResponse;
import com.mcardoso.srvcondominiopro.modules.condominios.dto.DashboardResponse;
import com.mcardoso.srvcondominiopro.modules.condominios.dto.UpdateCondominioRequest;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.modules.usuarios.UsuarioRepository;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import com.mcardoso.srvcondominiopro.shared.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CondominioService {

    private final CondominioRepository condominioRepository;
    private final UsuarioRepository usuarioRepository;

    public CondominioService(CondominioRepository condominioRepository, UsuarioRepository usuarioRepository) {
        this.condominioRepository = condominioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public CondominioResponse buscar(Long id, Usuario usuarioLogado) {
        validarAcesso(id, usuarioLogado);
        return CondominioResponse.from(buscarPorId(id));
    }

    @Transactional
    public CondominioResponse atualizar(Long id, UpdateCondominioRequest request, Usuario usuarioLogado) {
        validarAcesso(id, usuarioLogado);
        Condominio condominio = buscarPorId(id);

        condominio.setNome(request.nome());
        condominio.setEndereco(request.endereco());
        condominio.setTelefone(request.telefone());
        condominio.setLogoUrl(request.logoUrl());
        condominio.setNotificaEmail(request.notificaEmail());
        condominio.setNotificaWhatsapp(request.notificaWhatsapp());

        return CondominioResponse.from(condominio);
    }

    public DashboardResponse dashboard(Long id, Usuario usuarioLogado) {
        validarAcesso(id, usuarioLogado);
        buscarPorId(id);

        long totalUsuariosAtivos = usuarioRepository.countByCondominioIdAndAtivoTrue(id);
        long totalMoradores = usuarioRepository.countByCondominioIdAndAtivoTrueAndRoleIn(
                id, List.of(Role.PROPRIETARIO, Role.INQUILINO));

        return new DashboardResponse(totalUsuariosAtivos, totalMoradores);
    }

    private Condominio buscarPorId(Long id) {
        return condominioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Condomínio não encontrado"));
    }

    private void validarAcesso(Long id, Usuario usuarioLogado) {
        if (!usuarioLogado.getCondominio().getId().equals(id)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }
    }
}
