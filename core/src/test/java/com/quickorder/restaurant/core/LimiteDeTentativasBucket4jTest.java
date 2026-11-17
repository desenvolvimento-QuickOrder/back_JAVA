package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.quickorder.restaurant.core.equipe.infrastructure.LimiteDeTentativasBucket4j;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LimiteDeTentativasBucket4jTest {

  @Test
  void deveLimitarTrintaTentativasPorIpEmUmMinutoUsandoRelogioControlado() {
    var relogio = new RelogioControlado();
    relogio.ajustar(Instant.parse("2026-11-05T12:00:00Z"));
    var limite = new LimiteDeTentativasBucket4j(relogio);

    for (int tentativa = 0; tentativa < 30; tentativa++) {
      assertThat(limite.reservarTentativaPorIp("192.0.2.10", relogio.agora())).isZero();
    }

    assertThat(limite.reservarTentativaPorIp("192.0.2.10", relogio.agora())).isPositive();
    relogio.avancar(Duration.ofSeconds(2));
    assertThat(limite.reservarTentativaPorIp("192.0.2.10", relogio.agora())).isZero();
  }

  @Test
  void deveAplicarBloqueiosProgressivosEReiniciarQuandoHaSucesso() {
    var relogio = new RelogioControlado();
    relogio.ajustar(Instant.parse("2026-11-05T12:00:00Z"));
    var limite = new LimiteDeTentativasBucket4j(relogio);
    String conta = "restaurante:admin";

    registrarFalhas(limite, conta, relogio, 5);
    assertThat(limite.tempoRestanteDeBloqueioDaConta(conta, relogio.agora())).isEqualTo(60);
    relogio.avancar(Duration.ofMinutes(1));
    registrarFalhas(limite, conta, relogio, 5);
    assertThat(limite.tempoRestanteDeBloqueioDaConta(conta, relogio.agora())).isEqualTo(300);
    relogio.avancar(Duration.ofMinutes(5));
    registrarFalhas(limite, conta, relogio, 5);
    assertThat(limite.tempoRestanteDeBloqueioDaConta(conta, relogio.agora())).isEqualTo(900);

    limite.registrarSucessoDaConta(conta);
    registrarFalhas(limite, conta, relogio, 5);
    assertThat(limite.tempoRestanteDeBloqueioDaConta(conta, relogio.agora())).isEqualTo(60);
  }

  @Test
  void bloqueiaDispositivoAposVinteFalhasPorDezMinutos() {
    var relogio = new RelogioControlado();
    relogio.ajustar(Instant.parse("2026-11-05T12:00:00Z"));
    var limite = new LimiteDeTentativasBucket4j(relogio);

    for (int tentativa = 0; tentativa < 20; tentativa++) {
      limite.registrarFalhaDoDispositivo("hash-do-dispositivo", relogio.agora());
    }
    assertThat(limite.tempoRestanteDeBloqueioDoDispositivo("hash-do-dispositivo", relogio.agora()))
        .isEqualTo(600);

    relogio.avancar(Duration.ofMinutes(9).plusSeconds(59));
    assertThat(limite.tempoRestanteDeBloqueioDoDispositivo("hash-do-dispositivo", relogio.agora()))
        .isEqualTo(1);
    relogio.avancar(Duration.ofSeconds(1));
    assertThat(limite.tempoRestanteDeBloqueioDoDispositivo("hash-do-dispositivo", relogio.agora()))
        .isZero();
  }

  private static void registrarFalhas(
      LimiteDeTentativasBucket4j limite, String conta, RelogioControlado relogio, int quantidade) {
    for (int tentativa = 0; tentativa < quantidade; tentativa++) {
      limite.registrarFalhaDaConta(conta, relogio.agora());
    }
  }
}
