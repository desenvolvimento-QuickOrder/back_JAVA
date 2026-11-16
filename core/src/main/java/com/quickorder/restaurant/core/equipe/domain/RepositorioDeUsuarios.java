package com.quickorder.restaurant.core.equipe.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeUsuarios {

  Optional<Usuario> buscarPorLogin(String login);

  Optional<Usuario> buscarPorId(UUID id);

  boolean possuiAdminAtivo();

  boolean existeLogin(String login);

  List<Usuario> bloquearAdminsAtivos();

  PaginaDeUsuarios listar(Papel papel, Boolean ativo, int pagina, int tamanho);

  Usuario salvar(Usuario usuario);
}
