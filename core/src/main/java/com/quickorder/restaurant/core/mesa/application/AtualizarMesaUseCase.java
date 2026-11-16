package com.quickorder.restaurant.core.mesa.application;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaDuplicadoException;
import com.quickorder.restaurant.core.mesa.domain.MesaNotFoundException;
import com.quickorder.restaurant.core.mesa.domain.MesaOcupadaException;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AtualizarMesaUseCase {

  private final MesaRepository mesaRepository;
  private final MesaStatusResolver mesaStatusResolver;

  public AtualizarMesaUseCase(
      MesaRepository mesaRepository, MesaStatusResolver mesaStatusResolver) {
    this.mesaRepository = mesaRepository;
    this.mesaStatusResolver = mesaStatusResolver;
  }

  public Mesa execute(UUID id, String numero, Integer capacidade, Boolean ativa) {
    var mesa = mesaRepository.findById(id).orElseThrow(() -> new MesaNotFoundException(id));

    if (numero != null
        && !numero.equals(mesa.numero())
        && mesaRepository.existsByNumeroAndIdNot(numero, id)) {
      throw new MesaDuplicadoException(numero);
    }

    Mesa atualizada = mesa;
    if (numero != null) {
      atualizada = atualizada.alterarNumero(numero);
    }
    if (capacidade != null) {
      atualizada = atualizada.alterarCapacidade(capacidade);
    }
    if (ativa != null) {
      var statusAtual = mesaStatusResolver.resolve(id);
      if (!ativa && statusAtual == MesaStatus.OCUPADA) {
        throw new MesaOcupadaException(id);
      }
      atualizada = atualizada.alterarAtiva(ativa);
    }

    atualizada = atualizada.comStatus(mesaStatusResolver.resolve(id));
    return mesaRepository.save(atualizada);
  }
}
