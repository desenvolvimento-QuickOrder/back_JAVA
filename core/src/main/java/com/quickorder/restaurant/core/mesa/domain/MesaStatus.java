package com.quickorder.restaurant.core.mesa.domain;

public enum MesaStatus {
  LIVRE,
  OCUPADA;

  public static MesaStatus of(boolean ocupada) {
    return ocupada ? OCUPADA : LIVRE;
  }
}
