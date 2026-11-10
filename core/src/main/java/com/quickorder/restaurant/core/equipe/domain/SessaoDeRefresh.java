package com.quickorder.restaurant.core.equipe.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SessaoDeRefresh(
    UUID id,
    UUID restauranteId,
    UUID usuarioId,
    String tokenHash,
    UUID familiaId,
    Instant criadoEm,
    Instant expiraEm,
    Instant familiaExpiraEm,
    Instant usadoEm,
    Instant revogadoEm) {

  public SessaoDeRefresh {
    Objects.requireNonNull(id);
    Objects.requireNonNull(restauranteId);
    Objects.requireNonNull(usuarioId);
    Objects.requireNonNull(tokenHash);
    Objects.requireNonNull(familiaId);
    Objects.requireNonNull(criadoEm);
    Objects.requireNonNull(expiraEm);
    Objects.requireNonNull(familiaExpiraEm);
  }

  public SessaoDeRefresh marcarComoUsada(Instant instante) {
    return new SessaoDeRefresh(
        id,
        restauranteId,
        usuarioId,
        tokenHash,
        familiaId,
        criadoEm,
        expiraEm,
        familiaExpiraEm,
        instante,
        revogadoEm);
  }
}
