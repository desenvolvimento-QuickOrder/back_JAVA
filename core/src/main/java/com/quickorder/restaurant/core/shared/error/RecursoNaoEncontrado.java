package com.quickorder.restaurant.core.shared.error;

public class RecursoNaoEncontrado extends RuntimeException {

  public RecursoNaoEncontrado(String mensagem) {
    super(mensagem);
  }
}
