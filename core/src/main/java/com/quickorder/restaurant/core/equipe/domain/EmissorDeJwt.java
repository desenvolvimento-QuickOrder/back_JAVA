package com.quickorder.restaurant.core.equipe.domain;

import java.time.Instant;
import java.util.UUID;

public interface EmissorDeJwt {

  String emitir(Usuario usuario, Instant instante);

  default String emitir(
      Usuario usuario, Instant instante, UUID dispositivoId, boolean trocaPinPendente) {
    if (dispositivoId == null && !trocaPinPendente) {
      return emitir(usuario, instante);
    }
    return emitir(usuario, instante);
  }
}
