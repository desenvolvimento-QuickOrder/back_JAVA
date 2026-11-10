package com.quickorder.restaurant.core.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class ConfiguracaoDeCorsTest {

  @Test
  void deveExigirOrigemCorsForaDosPerfisDevETest() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    assertThatThrownBy(() -> new ConfiguracaoDeCors().corsConfigurationSource(environment, " , "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("QUICKORDER_CORS_ORIGINS");
  }

  @Test
  void deveUsarListaDeOrigemEHeadersCorsConfigurados() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    var source =
        new ConfiguracaoDeCors()
            .corsConfigurationSource(environment, "https://app.example, https://admin.example");
    var cors = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/teste"));

    assertThat(cors.getAllowedOrigins())
        .containsExactly("https://app.example", "https://admin.example");
    assertThat(cors.getAllowedMethods())
        .containsExactly("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");
    assertThat(cors.getAllowedHeaders())
        .containsExactly(
            "Authorization",
            "Content-Type",
            "X-Correlation-Id",
            "Idempotency-Key",
            "X-Device-Token");
    assertThat(cors.getExposedHeaders())
        .containsExactly("X-Correlation-Id", "Retry-After", "Location");
    assertThat(cors.getAllowCredentials()).isFalse();
  }

  @Test
  void devePermitirListaVaziaNosPerfisDevETest() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("test");

    assertThat(
            new ConfiguracaoDeCors()
                .corsConfigurationSource(environment, "")
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/")))
        .satisfies(cors -> assertThat(cors.getAllowedOrigins()).isEmpty());
  }
}
