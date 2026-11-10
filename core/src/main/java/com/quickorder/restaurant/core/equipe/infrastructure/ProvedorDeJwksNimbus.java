package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.ProvedorDeJwks;
import org.springframework.stereotype.Component;

@Component
public class ProvedorDeJwksNimbus implements ProvedorDeJwks {

  private final ConjuntoDeChavesJwt conjuntoDeChavesJwt;

  public ProvedorDeJwksNimbus(ConjuntoDeChavesJwt conjuntoDeChavesJwt) {
    this.conjuntoDeChavesJwt = conjuntoDeChavesJwt;
  }

  @Override
  public String obterJwksPublico() {
    return conjuntoDeChavesJwt.conjuntoPublico().toString();
  }
}
