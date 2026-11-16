package com.quickorder.restaurant.core.equipe.domain;

import java.util.regex.Pattern;

public record CodigoFuncionario(String valor) {

  private static final Pattern FORMATO = Pattern.compile("^\\d{4}$");

  public CodigoFuncionario {
    if (valor == null || !FORMATO.matcher(valor).matches()) {
      throw new IllegalArgumentException("Código deve conter quatro dígitos.");
    }
  }
}
