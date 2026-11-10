package com.quickorder.restaurant.core.equipe.domain;

public class LimiteDeTentativasExcedido extends RuntimeException {

  private final long retryAfterSeconds;

  public LimiteDeTentativasExcedido(long retryAfterSeconds) {
    super("Limite de tentativas excedido.");
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
