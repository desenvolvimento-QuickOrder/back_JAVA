package com.quickorder.restaurant.core.equipe.domain;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeUsuarios {

  Optional<Usuario> buscarPorLogin(String login);

  Optional<Usuario> buscarPorId(UUID id);

  boolean possuiAdminAtivo();

  Usuario salvar(Usuario usuario);
}
