package com.quickorder.restaurant.core.equipe.domain;

import java.time.Instant;

public interface LimiteDeTentativas {

  long reservarTentativaPorIp(String ip, Instant instante);

  long tempoRestanteDeBloqueioDaConta(String conta, Instant instante);

  void registrarFalhaDaConta(String conta, Instant instante);

  void registrarSucessoDaConta(String conta);

  default long tempoRestanteDeBloqueioDoDispositivo(String dispositivo, Instant instante) {
    return 0;
  }

  default void registrarFalhaDoDispositivo(String dispositivo, Instant instante) {}

  default void registrarSucessoDoDispositivo(String dispositivo) {}
}
