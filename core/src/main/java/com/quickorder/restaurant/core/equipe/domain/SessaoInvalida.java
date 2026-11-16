package com.quickorder.restaurant.core.equipe.domain;

public class SessaoInvalida extends RuntimeException {

  public SessaoInvalida() {
    super("A sessão não é válida.");
  }
}
