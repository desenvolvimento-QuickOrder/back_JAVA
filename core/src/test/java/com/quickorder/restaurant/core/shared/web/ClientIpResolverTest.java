package com.quickorder.restaurant.core.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

  @Test
  void deveResolverIpRealDoClienteDaPosicaoConfiguradaADireita() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.10");
    request.addHeader("X-Forwarded-For", "198.51.100.99, 203.0.113.25, 10.0.0.10");

    assertThat(new ClientIpResolver(2).resolver(request)).isEqualTo("203.0.113.25");
  }

  @Test
  void deveIgnorarEntradasInvalidasDaCadeiaDeEncaminhamento() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.10");
    request.addHeader("X-Forwarded-For", "nao-ip, 203.0.113.25, 10.0.0.10");

    assertThat(new ClientIpResolver(2).resolver(request)).isEqualTo("203.0.113.25");
  }

  @Test
  void deveUsarRemoteAddrQuandoCadeiaNaoTemPosicaoConfigurada() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.10");
    request.addHeader("X-Forwarded-For", "203.0.113.25");

    assertThat(new ClientIpResolver(2).resolver(request)).isEqualTo("10.0.0.10");
  }
}
