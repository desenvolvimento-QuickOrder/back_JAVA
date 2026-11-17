package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionario;
import com.quickorder.restaurant.core.equipe.domain.GeradorDeCodigoFuncionario;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeradorDeCodigoFuncionarioSeguro implements GeradorDeCodigoFuncionario {

  private final SecureRandom aleatorio = new SecureRandom();

  @Override
  public CodigoFuncionario sortear(List<CodigoFuncionario> codigosDisponiveis) {
    return codigosDisponiveis.get(aleatorio.nextInt(codigosDisponiveis.size()));
  }
}
