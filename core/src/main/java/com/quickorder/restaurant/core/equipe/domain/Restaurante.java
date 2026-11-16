package com.quickorder.restaurant.core.equipe.domain;

import java.util.Objects;
import java.util.UUID;

public record Restaurante(UUID id, String slug, boolean ativo) {

  public Restaurante {
    Objects.requireNonNull(id);
    Objects.requireNonNull(slug);
  }
}
