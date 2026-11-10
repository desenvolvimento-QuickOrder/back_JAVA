package com.quickorder.restaurant.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularidadeTest {

  @Test
  void deveRespeitarAsFronteirasDosModulos() {
    assertDoesNotThrow(() -> ApplicationModules.of(QuickOrderApplication.class).verify());
  }
}
