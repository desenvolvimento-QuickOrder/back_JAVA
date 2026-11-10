package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeUsuariosJpa implements RepositorioDeUsuarios {

  private final RepositorioJpaDeUsuarios repositorio;

  RepositorioDeUsuariosJpa(RepositorioJpaDeUsuarios repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public Optional<Usuario> buscarPorLogin(String login) {
    return repositorio
        .findByLoginAndPapelAndAtivoTrue(login, Papel.ADMIN)
        .map(UsuarioJpaEntity::paraDominio);
  }

  @Override
  public Optional<Usuario> buscarPorId(UUID id) {
    return repositorio.findById(id).map(UsuarioJpaEntity::paraDominio);
  }

  @Override
  public boolean possuiAdminAtivo() {
    return repositorio.existsByPapelAndAtivoTrue(Papel.ADMIN);
  }

  @Override
  public Usuario salvar(Usuario usuario) {
    return repositorio.saveAndFlush(new UsuarioJpaEntity(usuario)).paraDominio();
  }
}
