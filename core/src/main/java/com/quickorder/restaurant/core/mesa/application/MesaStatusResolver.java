package com.quickorder.restaurant.core.mesa.application;

import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import java.util.UUID;

public interface MesaStatusResolver {
  MesaStatus resolve(UUID mesaId);
}
