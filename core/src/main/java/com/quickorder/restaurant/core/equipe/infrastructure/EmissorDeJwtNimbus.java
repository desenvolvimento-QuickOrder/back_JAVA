package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.application.CriadorDeSessao;
import com.quickorder.restaurant.core.equipe.domain.EmissorDeJwt;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class EmissorDeJwtNimbus implements EmissorDeJwt {

  private final JwtEncoder encoder;
  private final String emissor;
  private final String identificadorDaChave;

  public EmissorDeJwtNimbus(
      JwtEncoder encoder,
      @Value("${quickorder.jwt.issuer}") String emissor,
      @Value("${quickorder.jwt.key-id}") String identificadorDaChave) {
    this.encoder = encoder;
    this.emissor = emissor;
    this.identificadorDaChave = identificadorDaChave;
  }

  @Override
  public String emitir(Usuario usuario, Instant instante) {
    Instant expiracao = instante.plusSeconds(CriadorDeSessao.DURACAO_ACCESS_TOKEN_SEGUNDOS);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(emissor)
            .subject(usuario.id().toString())
            .issuedAt(instante)
            .expiresAt(expiracao)
            .id(UUID.randomUUID().toString())
            .claim("tenant_id", usuario.restauranteId().toString())
            .claim("roles", List.of(usuario.papel().name()))
            .build();
    JwsHeader cabecalho =
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(identificadorDaChave).build();
    return encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
  }
}
