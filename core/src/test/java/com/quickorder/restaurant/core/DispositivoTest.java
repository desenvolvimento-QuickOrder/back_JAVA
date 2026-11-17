package com.quickorder.restaurant.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickorder.restaurant.core.equipe.domain.Dispositivo;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DispositivoTest {

  @Test
  void registraUsoRevogaEPermaneceRevogadoIdempotentemente() {
    Instant criadoEm = Instant.parse("2026-01-01T00:00:00Z");
    Dispositivo dispositivo =
        new Dispositivo(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Tablet",
            "a".repeat(64),
            UUID.randomUUID(),
            criadoEm,
            null,
            null);
    Dispositivo usado = dispositivo.marcarUso(criadoEm.plusSeconds(10));
    assertTrue(usado.ativo());
    assertTrue(usado.ultimoUsoEm().isAfter(criadoEm));

    Dispositivo revogado = usado.revogar(criadoEm.plusSeconds(20));
    assertFalse(revogado.ativo());
    assertSame(revogado, revogado.revogar(criadoEm.plusSeconds(30)));
  }
}
