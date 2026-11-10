package com.quickorder.restaurant.core.shared.error;

public class ExcecaoDeNegocio extends RuntimeException {

  private final String code;

  public ExcecaoDeNegocio(String code, String mensagem) {
    super(mensagem);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
