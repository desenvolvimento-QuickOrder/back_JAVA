package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.shared.error.TraducaoDeConstraint;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TradutorDeConstraintsDaEquipe implements TraducaoDeConstraint {

  private static final Map<String, String> CODIGOS =
      Map.of("uq_usuario_login", "LOGIN_JA_EXISTE", "uq_usuario_codigo", "CODIGO_JA_EXISTE");

  @Override
  public Optional<String> traduzir(String nomeDaConstraint) {
    return Optional.ofNullable(CODIGOS.get(nomeDaConstraint));
  }
}
