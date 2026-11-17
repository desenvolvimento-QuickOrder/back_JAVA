package com.quickorder.restaurant.core.mesa.application;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaNotFoundException;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObterMesaUseCase {

  private final MesaRepository mesaRepository;
  private final MesaStatusResolver mesaStatusResolver;

  public ObterMesaUseCase(MesaRepository mesaRepository, MesaStatusResolver mesaStatusResolver) {
    this.mesaRepository = mesaRepository;
    this.mesaStatusResolver = mesaStatusResolver;
  }

  @Transactional(readOnly = true)
  public Mesa execute(UUID id) {
    return mesaRepository
        .findById(id)
        .map(mesa -> mesa.comStatus(mesaStatusResolver.resolve(mesa.id())))
        .orElseThrow(() -> new MesaNotFoundException(id));
  }
}
