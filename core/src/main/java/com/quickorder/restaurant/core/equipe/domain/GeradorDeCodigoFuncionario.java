package com.quickorder.restaurant.core.equipe.domain;

import java.util.List;

@FunctionalInterface
public interface GeradorDeCodigoFuncionario {

  CodigoFuncionario sortear(List<CodigoFuncionario> codigosDisponiveis);
}
