package com.quickorder.restaurant.core.mesa.domain;

import java.util.UUID;

public record Mesa(UUID id, String numero, Integer capacidade, Boolean ativa, MesaStatus status) {

  public Mesa {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (numero == null || numero.isBlank()) {
      throw new IllegalArgumentException("NUMERO_OBRIGATORIO");
    }
    numero = numero.trim();
    if (numero.length() > 20) {
      throw new IllegalArgumentException("NUMERO_INVALIDO");
    }
    if (capacidade == null || capacidade <= 0 || capacidade > 100) {
      throw new IllegalArgumentException("CAPACIDADE_INVALIDA");
    }
    if (ativa == null) {
      ativa = true;
    }
    if (status == null) {
      status = MesaStatus.LIVRE;
    }
  }

  public static Mesa criar(String numero, Integer capacidade) {
    return new Mesa(UUID.randomUUID(), numero, capacidade, true, MesaStatus.LIVRE);
  }

  public Mesa alterarNumero(String novoNumero) {
    return new Mesa(id, novoNumero, capacidade, ativa, status);
  }

  public Mesa alterarCapacidade(Integer novaCapacidade) {
    return new Mesa(id, numero, novaCapacidade, ativa, status);
  }

  public Mesa alterarAtiva(Boolean novaAtiva) {
    return new Mesa(id, numero, capacidade, novaAtiva, status);
  }

  public Mesa comStatus(MesaStatus novoStatus) {
    return new Mesa(id, numero, capacidade, ativa, novoStatus);
  }
}
