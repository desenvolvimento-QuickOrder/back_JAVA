package com.quickorder.restaurant.core.mesa.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MesaRepository {
  Mesa save(Mesa mesa);

  Optional<Mesa> findById(UUID id);

  Optional<Mesa> findByIdForUpdate(UUID id);

  List<Mesa> findPage(int offset, int limit, MesaStatus status, Boolean ativa);

  long count(MesaStatus status, Boolean ativa);

  boolean existsByNumero(String numero);

  boolean existsByNumeroAndIdNot(String numero, UUID id);
}
