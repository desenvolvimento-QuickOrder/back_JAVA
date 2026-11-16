package com.quickorder.restaurant.core.mesa.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MesaRepository {
  Mesa save(Mesa mesa);

  Optional<Mesa> findById(UUID id);

  List<Mesa> findAll();

  boolean existsByNumero(String numero);

  boolean existsByNumeroAndIdNot(String numero, UUID id);
}
