package com.quickorder.restaurant.core.mesa.web;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import org.springframework.stereotype.Component;

@Component
public class MesaMapper {
  public MesaResponse toResponse(Mesa mesa) {
    return new MesaResponse(
        mesa.id(), mesa.numero(), mesa.capacidade(), mesa.status(), mesa.ativa());
  }
}
