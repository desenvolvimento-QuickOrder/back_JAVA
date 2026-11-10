package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "quickorder.jwt.previous-public-key-location=classpath:keys/test-second-public.pem",
      "quickorder.jwt.previous-key-id=teste-anterior"
    })
@AutoConfigureMockMvc
class RotacaoDeChavesJwtIT extends IntegracaoBaseIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtDecoder jwtDecoder;

  @Test
  void publicaChaveAtualEAnteriorEPermiteValidacaoIndependenteDoCore() throws Exception {
    MvcResult resultado = mockMvc.perform(get("/.well-known/jwks.json")).andReturn();
    assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    assertThat(resultado.getResponse().getContentType()).contains("application/json");

    JWKSet conjunto = JWKSet.parse(resultado.getResponse().getContentAsString());
    assertThat(conjunto.getKeys())
        .extracting(chave -> chave.getKeyID())
        .containsExactlyInAnyOrder("teste-atual", "teste-anterior");

    Instant agora = Instant.now();
    String tokenAtual = assinar(chavePrivada("keys/test-private.pem"), "teste-atual", agora);
    String tokenAnterior =
        assinar(chavePrivada("keys/test-second-private.pem"), "teste-anterior", agora);
    assertThat(jwtDecoder.decode(tokenAtual).getIssuer()).hasToString("https://quickorder");
    assertThat(jwtDecoder.decode(tokenAnterior).getIssuer()).hasToString("https://quickorder");

    JWKSource<SecurityContext> fonteExterna = (seletor, contexto) -> seletor.select(conjunto);
    NimbusJwtDecoder decoderExterno =
        NimbusJwtDecoder.withJwkSource(fonteExterna).jwsAlgorithm(SignatureAlgorithm.RS256).build();
    decoderExterno.setJwtValidator(JwtValidators.createDefaultWithIssuer("https://quickorder"));
    assertThat(decoderExterno.decode(tokenAnterior).getClaimAsString("tenant_id")).isNotBlank();
    assertThat(
            mockMvc
                .perform(
                    get("/api/v1/mesas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAnterior))
                .andReturn()
                .getResponse()
                .getStatus())
        .isEqualTo(404);
  }

  private static String assinar(RSAPrivateKey chave, String kid, Instant instante)
      throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer("https://quickorder")
            .subject(UUID.randomUUID().toString())
            .issueTime(Date.from(instante))
            .expirationTime(Date.from(instante.plus(Duration.ofMinutes(15))))
            .jwtID(UUID.randomUUID().toString())
            .claim("tenant_id", UUID.randomUUID().toString())
            .claim("roles", List.of("ADMIN"))
            .build();
    SignedJWT token =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).type(JOSEObjectType.JWT).build(),
            claims);
    token.sign(new RSASSASigner(chave));
    return token.serialize();
  }

  private RSAPrivateKey chavePrivada(String caminho) throws Exception {
    try (var entrada = getClass().getClassLoader().getResourceAsStream(caminho)) {
      return RsaKeyConverters.pkcs8().convert(entrada);
    }
  }
}
