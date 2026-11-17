package com.quickorder.restaurant.core.equipe.domain;

import java.time.Instant;
import java.util.UUID;

public record Dispositivo(
    UUID id,
    UUID restauranteId,
    String nome,
    String tokenHash,
    UUID criadoPor,
    Instant criadoEm,
    Instant ultimoUsoEm,
    Instant revogadoEm) {

  public boolean ativo() {
    return revogadoEm == null;
  }

  public Dispositivo marcarUso(Instant instante) {
    return new Dispositivo(
        id, restauranteId, nome, tokenHash, criadoPor, criadoEm, instante, revogadoEm);
  }

  public Dispositivo revogar(Instant instante) {
    return ativo()
        ? new Dispositivo(
            id, restauranteId, nome, tokenHash, criadoPor, criadoEm, ultimoUsoEm, instante)
        : this;
  }
}
