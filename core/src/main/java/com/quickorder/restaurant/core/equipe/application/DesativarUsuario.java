package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeSessoesDeRefresh;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.ExcecaoDeNegocio;
import com.quickorder.restaurant.core.shared.error.RecursoNaoEncontrado;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesativarUsuario {

  private final RepositorioDeUsuarios usuarios;
  private final RepositorioDeSessoesDeRefresh sessoes;
  private final Relogio relogio;

  public DesativarUsuario(
      RepositorioDeUsuarios usuarios, RepositorioDeSessoesDeRefresh sessoes, Relogio relogio) {
    this.usuarios = usuarios;
    this.sessoes = sessoes;
    this.relogio = relogio;
  }

  @Transactional
  public Usuario executar(UUID id) {
    Usuario usuario =
        usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontrado("Usuário"));
    if (usuario.ativo() && usuario.papel() == Papel.ADMIN) {
      if (usuarios.bloquearAdminsAtivos().size() <= 1) {
        throw new ExcecaoDeNegocio("ULTIMO_ADMIN", "Não é permitido desativar o último ADMIN.");
      }
    }
    if (!usuario.ativo()) {
      return usuario;
    }
    Usuario salvo = usuarios.salvar(usuario.desativar());
    sessoes.revogarTodasDoUsuario(id, relogio.agora());
    return salvo;
  }
}
