package com.quickorder.restaurant.core.equipe.domain;

public class CodigoFuncionarioDuplicado extends RuntimeException {

  public CodigoFuncionarioDuplicado() {
    super("O código do funcionário já está em uso.");
  }
}
