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
    String codigo,
    String pinHash,
    boolean deveTrocarPin,
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
    this(id, restauranteId, nome, login, senhaHash, papel, ativo, null, null, false, 0);
  }

  public Usuario(
      UUID id,
      UUID restauranteId,
      String nome,
      String login,
      String senhaHash,
      Papel papel,
      boolean ativo,
      long versao) {
    this(id, restauranteId, nome, login, senhaHash, papel, ativo, null, null, false, versao);
  }

  public static Usuario novoAdmin(
      UUID id, UUID restauranteId, String nome, String login, String senhaHash) {
    return new Usuario(id, restauranteId, nome, login, senhaHash, Papel.ADMIN, true, 0);
  }

  public static Usuario novoFuncionario(
      UUID id, UUID restauranteId, String nome, Papel papel, String codigo, String pinHash) {
    if (papel != Papel.GARCOM && papel != Papel.COPA) {
      throw new IllegalArgumentException("Funcionário deve ser GARCOM ou COPA.");
    }
    return new Usuario(id, restauranteId, nome, null, null, papel, true, codigo, pinHash, true, 0);
  }

  public Usuario renomear(String novoNome) {
    return new Usuario(
        id,
        restauranteId,
        novoNome,
        login,
        senhaHash,
        papel,
        ativo,
        codigo,
        pinHash,
        deveTrocarPin,
        versao);
  }

  public Usuario trocarSenha(String novoHash) {
    return new Usuario(
        id,
        restauranteId,
        nome,
        login,
        novoHash,
        papel,
        ativo,
        codigo,
        pinHash,
        deveTrocarPin,
        versao);
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
    return new Usuario(
        id,
        restauranteId,
        nome,
        login,
        senhaHash,
        novoPapel,
        ativo,
        codigo,
        pinHash,
        deveTrocarPin,
        versao);
  }

  public Usuario trocarPin(String novoHash) {
    return new Usuario(
        id, restauranteId, nome, login, senhaHash, papel, ativo, codigo, novoHash, false, versao);
  }

  public Usuario redefinirPin(String novoHash) {
    return new Usuario(
        id, restauranteId, nome, login, senhaHash, papel, ativo, codigo, novoHash, true, versao);
  }

  public Usuario desativar() {
    return ativo
        ? new Usuario(
            id,
            restauranteId,
            nome,
            login,
            senhaHash,
            papel,
            false,
            codigo,
            pinHash,
            deveTrocarPin,
            versao)
        : this;
  }

  public Usuario ativar() {
    return ativo
        ? this
        : new Usuario(
            id,
            restauranteId,
            nome,
            login,
            senhaHash,
            papel,
            true,
            codigo,
            pinHash,
            deveTrocarPin,
            versao);
  }
}
