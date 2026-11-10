package com.quickorder.restaurant.core.equipe.domain;

public interface GeradorDeTokenOpaco {

  String gerar();

  String hash(String token);
}
