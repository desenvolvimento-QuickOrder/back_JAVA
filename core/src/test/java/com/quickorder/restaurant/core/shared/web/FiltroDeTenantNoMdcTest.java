package com.quickorder.restaurant.core.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class FiltroDeTenantNoMdcTest {

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  void deveAdicionarTenantDoJwtAoMdcERemoveLoAoFinalizar() throws Exception {
    String tenantId = "07fbd6c1-7cdb-4f37-b49f-60bd7bda10d8";
    Jwt jwt =
        Jwt.withTokenValue("teste").header("alg", "RS256").claim("tenant_id", tenantId).build();
    var contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(new JwtAuthenticationToken(jwt));
    SecurityContextHolder.setContext(contexto);

    new FiltroDeTenantNoMdc()
        .doFilter(
            new MockHttpServletRequest(),
            new MockHttpServletResponse(),
            (request, response) -> assertThat(MDC.get("tenant_id")).isEqualTo(tenantId));

    assertThat(MDC.get("tenant_id")).isNull();
  }
}
