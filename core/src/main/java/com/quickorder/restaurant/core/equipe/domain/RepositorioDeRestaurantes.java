package com.quickorder.restaurant.core.equipe.domain;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeRestaurantes {

  Optional<Restaurante> buscarPorSlug(String slug);

  Optional<Restaurante> buscarPorId(UUID id);
}
