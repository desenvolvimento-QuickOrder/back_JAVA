package com.quickorder.restaurant.core.shared.error;

public class ConflitoDeNegocio extends RuntimeException {

  private final String code;

  public ConflitoDeNegocio(String code, String mensagem) {
    super(mensagem);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
