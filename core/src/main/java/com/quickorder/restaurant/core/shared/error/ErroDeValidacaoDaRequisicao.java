package com.quickorder.restaurant.core.shared.error;

import java.util.List;

public class ErroDeValidacaoDaRequisicao extends RuntimeException {

  private final List<ErroDeCampo> erros;

  public ErroDeValidacaoDaRequisicao(List<ErroDeCampo> erros) {
    super("A requisição contém dados inválidos.");
    this.erros = List.copyOf(erros);
  }

  public List<ErroDeCampo> getErros() {
    return erros;
  }
}
