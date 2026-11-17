package com.quickorder.restaurant.core.equipe.domain;

import java.util.regex.Pattern;

public record Pin(String valor) {

  private static final Pattern FORMATO = Pattern.compile("^\\d{4}$");

  public Pin {
    if (valor == null || !FORMATO.matcher(valor).matches()) {
      throw new IllegalArgumentException("PIN deve conter quatro dígitos.");
    }
  }

  public boolean podeSerUsadoCom(CodigoFuncionario codigo) {
    return !valor.equals(codigo.valor()) && podeSerUsado();
  }

  public boolean podeSerUsado() {
    if (valor.chars().distinct().count() == 1) {
      return false;
    }
    int passo = valor.charAt(1) - valor.charAt(0);
    return !(Math.abs(passo) == 1
        && valor.charAt(2) - valor.charAt(1) == passo
        && valor.charAt(3) - valor.charAt(2) == passo);
  }
}
