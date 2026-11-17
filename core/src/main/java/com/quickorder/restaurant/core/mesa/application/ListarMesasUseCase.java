package com.quickorder.restaurant.core.mesa.application;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarMesasUseCase {

  private final MesaRepository mesaRepository;

  public ListarMesasUseCase(MesaRepository mesaRepository) {
    this.mesaRepository = mesaRepository;
  }

  @Transactional(readOnly = true)
  public MesaPage<Mesa> execute(Integer page, Integer size, MesaStatus status, Boolean ativa) {
    int pagina = page == null || page < 0 ? 0 : page;
    int tamanho = size == null || size <= 0 ? 20 : size;
    if (tamanho > 100) {
      throw new IllegalArgumentException("TAMANHO_PAGINA_INVALIDO");
    }
    long total = mesaRepository.count(status, ativa);
    long offset = (long) pagina * tamanho;
    var mesas = mesaRepository.findPage(offset, tamanho, status, ativa);
    return MesaPage.from(mesas, pagina, tamanho, total);
  }
}
