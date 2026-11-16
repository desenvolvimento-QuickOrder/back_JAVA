package com.quickorder.restaurant.core.mesa.domain;

import java.util.UUID;

public class MesaNotFoundException extends RuntimeException {
  public MesaNotFoundException(UUID id) {
    super("Mesa não encontrada: " + id);
  }
}
