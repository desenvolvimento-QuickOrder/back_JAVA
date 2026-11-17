package com.quickorder.restaurant.core.equipe.domain;

public record ResultadoDeSessao(
    String accessToken, String refreshToken, int expiresIn, boolean deveTrocarPin) {

  public ResultadoDeSessao(String accessToken, String refreshToken, int expiresIn) {
    this(accessToken, refreshToken, expiresIn, false);
  }
}
