package com.quickorder.restaurant.core.mesa.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMesaRepository extends JpaRepository<MesaEntity, UUID> {
  boolean existsByNumeroAndRestauranteId(String numero, UUID restauranteId);

  boolean existsByNumeroAndIdNotAndRestauranteId(String numero, UUID id, UUID restauranteId);
}
