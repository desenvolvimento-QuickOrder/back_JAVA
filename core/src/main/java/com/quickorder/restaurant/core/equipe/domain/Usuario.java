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
    boolean ativo) {

  public Usuario {
    Objects.requireNonNull(id);
    Objects.requireNonNull(restauranteId);
    Objects.requireNonNull(papel);
    if (nome == null || nome.isBlank() || login == null || login.isBlank()) {
      throw new IllegalArgumentException("Nome e login são obrigatórios.");
    }
    login = login.trim().toLowerCase(Locale.ROOT);
    Objects.requireNonNull(senhaHash);
  }

  public static Usuario novoAdmin(
      UUID id, UUID restauranteId, String nome, String login, String senhaHash) {
    return new Usuario(id, restauranteId, nome, login, senhaHash, Papel.ADMIN, true);
  }
}
