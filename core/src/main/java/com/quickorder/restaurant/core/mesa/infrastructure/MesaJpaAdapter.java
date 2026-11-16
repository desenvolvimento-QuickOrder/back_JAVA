package com.quickorder.restaurant.core.mesa.infrastructure;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MesaJpaAdapter implements MesaRepository {

  private final JpaMesaRepository jpaMesaRepository;

  public MesaJpaAdapter(JpaMesaRepository jpaMesaRepository) {
    this.jpaMesaRepository = jpaMesaRepository;
  }

  @Override
  public Mesa save(Mesa mesa) {
    return jpaMesaRepository.save(MesaEntity.fromDomain(mesa)).toDomain();
  }

  @Override
  public Optional<Mesa> findById(UUID id) {
    return jpaMesaRepository.findById(id).map(MesaEntity::toDomain);
  }

  @Override
  public List<Mesa> findAll() {
    return jpaMesaRepository.findAll().stream().map(MesaEntity::toDomain).toList();
  }

  @Override
  public boolean existsByNumero(String numero) {
    return jpaMesaRepository.existsByNumeroAndRestauranteId(
        numero, MesaEntity.DEFAULT_RESTAURANTE_ID);
  }

  @Override
  public boolean existsByNumeroAndIdNot(String numero, UUID id) {
    return jpaMesaRepository.existsByNumeroAndIdNotAndRestauranteId(
        numero, id, MesaEntity.DEFAULT_RESTAURANTE_ID);
  }
}
