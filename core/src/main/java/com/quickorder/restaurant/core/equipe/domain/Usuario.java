package com.quickorder.restaurant.core.equipe.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record Usuario(
    UUID id,
    UUID restauranteId,
    String nome,
    String login,
    String senhaHash,
    Papel papel,
    boolean ativo,
    long versao) {

  public Usuario {
    Objects.requireNonNull(id);
    Objects.requireNonNull(restauranteId);
    Objects.requireNonNull(papel);
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("Nome é obrigatório.");
    }
    if (login != null) {
      login = login.trim().toLowerCase(Locale.ROOT);
    }
  }

  public Usuario(
      UUID id,
      UUID restauranteId,
      String nome,
      String login,
      String senhaHash,
      Papel papel,
      boolean ativo) {
    this(id, restauranteId, nome, login, senhaHash, papel, ativo, 0);
  }

  public static Usuario novoAdmin(
      UUID id, UUID restauranteId, String nome, String login, String senhaHash) {
    return new Usuario(id, restauranteId, nome, login, senhaHash, Papel.ADMIN, true, 0);
  }

  public Usuario renomear(String novoNome) {
    return new Usuario(id, restauranteId, novoNome, login, senhaHash, papel, ativo, versao);
  }

  public Usuario trocarSenha(String novoHash) {
    return new Usuario(id, restauranteId, nome, login, novoHash, papel, ativo, versao);
  }

  public Usuario trocarPapel(Papel novoPapel) {
    if (novoPapel == papel) {
      return this;
    }
    boolean trocaPermitida =
        (papel == Papel.GARCOM && novoPapel == Papel.COPA)
            || (papel == Papel.COPA && novoPapel == Papel.GARCOM);
    if (!trocaPermitida) {
      throw new TrocaDePapelNaoPermitida();
    }
    return new Usuario(id, restauranteId, nome, login, senhaHash, novoPapel, ativo, versao);
  }

  public Usuario desativar() {
    return ativo
        ? new Usuario(id, restauranteId, nome, login, senhaHash, papel, false, versao)
        : this;
  }

  public Usuario ativar() {
    return ativo
        ? this
        : new Usuario(id, restauranteId, nome, login, senhaHash, papel, true, versao);
  }
}
