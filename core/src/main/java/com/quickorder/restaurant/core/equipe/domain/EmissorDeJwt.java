package com.quickorder.restaurant.core.equipe.domain;

import java.time.Instant;

public interface EmissorDeJwt {

  String emitir(Usuario usuario, Instant instante);
}
