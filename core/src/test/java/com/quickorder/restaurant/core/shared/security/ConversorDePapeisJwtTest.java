package com.quickorder.restaurant.core.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class ConversorDePapeisJwtTest {

  @Test
  void deveConverterPapeisDaClaimRolesEmAuthoritiesComPrefixoRole() {
    Jwt jwt =
        Jwt.withTokenValue("teste")
            .header("alg", "RS256")
            .claim("roles", List.of("ADMIN", "ATENDENTE"))
            .build();

    assertThat(new ConversorDePapeisJwt().convert(jwt))
        .extracting("authority")
        .containsExactly("ROLE_ADMIN", "ROLE_ATENDENTE");
  }
}
