package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.RecursoNaoEncontrado;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtivarUsuario {

  private final RepositorioDeUsuarios usuarios;

  public AtivarUsuario(RepositorioDeUsuarios usuarios) {
    this.usuarios = usuarios;
  }

  @Transactional
  public Usuario executar(UUID id) {
    Usuario usuario =
        usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontrado("Usuário"));
    return usuario.ativo() ? usuario : usuarios.salvar(usuario.ativar());
  }
}
