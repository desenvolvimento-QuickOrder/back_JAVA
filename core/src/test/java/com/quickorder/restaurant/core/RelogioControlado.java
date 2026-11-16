package com.quickorder.restaurant.core;

import com.quickorder.restaurant.core.equipe.domain.Relogio;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class RelogioControlado implements Relogio {

  private final AtomicReference<Instant> instante = new AtomicReference<>(Instant.now());

  @Override
  public Instant agora() {
    return instante.get();
  }

  public void ajustar(Instant novoInstante) {
    instante.set(novoInstante);
  }

  public void avancar(Duration duracao) {
    instante.updateAndGet(atual -> atual.plus(duracao));
  }
}
