package com.quickorder.restaurant.core.mesa.application;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaDuplicadoException;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CriarMesaUseCase {

  private final MesaRepository mesaRepository;

  public CriarMesaUseCase(MesaRepository mesaRepository) {
    this.mesaRepository = mesaRepository;
  }

  @Transactional
  public Mesa execute(String numero, Integer capacidade) {
    var mesa = Mesa.criar(numero, capacidade);
    if (mesaRepository.existsByNumero(mesa.numero())) {
      throw new MesaDuplicadoException(mesa.numero());
    }
    return mesaRepository.save(mesa);
  }
}
