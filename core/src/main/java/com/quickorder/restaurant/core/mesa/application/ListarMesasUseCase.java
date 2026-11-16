package com.quickorder.restaurant.core.mesa.application;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListarMesasUseCase {

  private final MesaRepository mesaRepository;
  private final MesaStatusResolver mesaStatusResolver;

  public ListarMesasUseCase(MesaRepository mesaRepository, MesaStatusResolver mesaStatusResolver) {
    this.mesaRepository = mesaRepository;
    this.mesaStatusResolver = mesaStatusResolver;
  }

  public MesaPage<Mesa> execute(Integer page, Integer size, MesaStatus status, Boolean ativa) {
    int pagina = page == null || page < 0 ? 0 : page;
    int tamanho = size == null || size <= 0 ? 20 : Math.min(size, 100);

    List<Mesa> mesas =
        mesaRepository.findAll().stream()
            .map(mesa -> mesa.comStatus(mesaStatusResolver.resolve(mesa.id())))
            .filter(mesa -> status == null || mesa.status() == status)
            .filter(mesa -> ativa == null || mesa.ativa().equals(ativa))
            .toList();

    int fromIndex = Math.min(pagina * tamanho, mesas.size());
    int toIndex = Math.min(fromIndex + tamanho, mesas.size());
    var paginaMesas = mesas.subList(fromIndex, toIndex);

    return MesaPage.from(paginaMesas, pagina, tamanho, mesas.size());
  }
}
