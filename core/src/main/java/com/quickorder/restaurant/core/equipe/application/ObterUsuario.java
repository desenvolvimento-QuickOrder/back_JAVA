package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.RecursoNaoEncontrado;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObterUsuario {

  private final RepositorioDeUsuarios usuarios;

  public ObterUsuario(RepositorioDeUsuarios usuarios) {
    this.usuarios = usuarios;
  }

  @Transactional(readOnly = true)
  public Usuario executar(UUID id) {
    return usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontrado("Usuário"));
  }
}
