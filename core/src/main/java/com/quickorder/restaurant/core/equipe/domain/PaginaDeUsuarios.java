package com.quickorder.restaurant.core.equipe.domain;

import java.util.List;

public record PaginaDeUsuarios(int pagina, int tamanho, long total, List<Usuario> itens) {

  public PaginaDeUsuarios {
    itens = List.copyOf(itens);
  }
}
