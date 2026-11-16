package com.quickorder.restaurant.core.mesa.domain;

import java.util.UUID;

public class MesaOcupadaException extends RuntimeException {
  public MesaOcupadaException(UUID id) {
    super("Não é possível desativar uma mesa ocupada: " + id);
  }
}
