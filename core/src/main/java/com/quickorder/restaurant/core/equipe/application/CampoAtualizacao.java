package com.quickorder.restaurant.core.equipe.application;

public record CampoAtualizacao<T>(boolean informado, T valor) {

  public static <T> CampoAtualizacao<T> ausente() {
    return new CampoAtualizacao<>(false, null);
  }

  public static <T> CampoAtualizacao<T> informado(T valor) {
    return new CampoAtualizacao<>(true, valor);
  }
}
