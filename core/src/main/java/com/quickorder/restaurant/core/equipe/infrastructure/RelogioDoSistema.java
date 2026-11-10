package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.Relogio;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class RelogioDoSistema implements Relogio {

  @Override
  public Instant agora() {
    return Instant.now();
  }
}
