package com.quickorder.restaurant.core.mesa.domain;

public class MesaDuplicadoException extends RuntimeException {
  public MesaDuplicadoException(String numero) {
    super("Mesa já cadastrada com o número: " + numero);
  }
}
