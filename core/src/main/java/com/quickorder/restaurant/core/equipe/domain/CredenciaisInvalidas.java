package com.quickorder.restaurant.core.equipe.domain;

public class CredenciaisInvalidas extends RuntimeException {

  public CredenciaisInvalidas() {
    super("Credenciais inválidas.");
  }
}
