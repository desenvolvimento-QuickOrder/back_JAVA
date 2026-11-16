package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quickorder.restaurant.core.equipe.infrastructure.HashDeSenhaBcryptHmac;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class HashDeSenhaBcryptHmacTest {

  @Test
  void deveAplicarHmacAntesDoBcryptEValidarACombinacaoCorreta() {
    var hash = new HashDeSenhaBcryptHmac("pepper-de-teste", new MockEnvironment());
    String resultado = hash.gerar("senha-segura");

    assertThat(resultado).startsWith("{bcrypt-hmac-v1}$2");
    assertThat(hash.corresponde("senha-segura", resultado)).isTrue();
    assertThat(hash.corresponde("senha-incorreta", resultado)).isFalse();
    assertThat(
            new HashDeSenhaBcryptHmac("outro-pepper", new MockEnvironment())
                .corresponde("senha-segura", resultado))
        .isFalse();
  }

  @Test
  void deveExigirPepperForaDosPerfisDeDesenvolvimentoETeste() {
    assertThatThrownBy(() -> new HashDeSenhaBcryptHmac("", new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("QUICKORDER_SENHA_PEPPER");
  }
}
