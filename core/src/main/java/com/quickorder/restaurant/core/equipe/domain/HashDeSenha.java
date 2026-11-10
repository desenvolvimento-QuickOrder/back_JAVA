package com.quickorder.restaurant.core.equipe.domain;

public interface HashDeSenha {

  String gerar(String senha);

  boolean corresponde(String senha, String hash);
}
