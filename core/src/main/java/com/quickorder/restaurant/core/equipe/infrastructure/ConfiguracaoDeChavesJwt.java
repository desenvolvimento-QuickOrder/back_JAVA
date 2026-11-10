package com.quickorder.restaurant.core.equipe.infrastructure;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoDeChavesJwt {

  @Bean
  ConjuntoDeChavesJwt conjuntoDeChavesJwt(
      ResourceLoader resourceLoader,
      @Value("${quickorder.jwt.private-key-location:}") String localChavePrivada,
      @Value("${quickorder.jwt.key-id:}") String identificadorAtual,
      @Value("${quickorder.jwt.previous-public-key-location:}") String localChaveAnterior,
      @Value("${quickorder.jwt.previous-key-id:}") String identificadorAnterior) {
    exigirPreenchido(localChavePrivada, "JWT_PRIVATE_KEY_LOCATION");
    exigirPreenchido(identificadorAtual, "JWT_KEY_ID");

    try {
      RSAPrivateKey chavePrivada = carregarChavePrivada(resourceLoader, localChavePrivada);
      RSAPublicKey chavePublicaAtual = derivarChavePublica(chavePrivada);
      RSAKey chaveAtual =
          new RSAKey.Builder(chavePublicaAtual)
              .privateKey(chavePrivada)
              .keyID(identificadorAtual)
              .keyUse(KeyUse.SIGNATURE)
              .algorithm(JWSAlgorithm.RS256)
              .build();

      List<JWK> chaves = new ArrayList<>();
      chaves.add(chaveAtual);
      List<JWK> chavesPublicas = new ArrayList<>();
      chavesPublicas.add(chaveAtual.toPublicJWK());

      boolean possuiLocalAnterior = !localChaveAnterior.isBlank();
      boolean possuiIdentificadorAnterior = !identificadorAnterior.isBlank();
      if (possuiLocalAnterior != possuiIdentificadorAnterior) {
        throw new IllegalStateException(
            "JWT_PREVIOUS_PUBLIC_KEY_LOCATION e JWT_PREVIOUS_KEY_ID devem ser configurados juntos.");
      }
      if (possuiLocalAnterior) {
        RSAPublicKey chaveAnterior = carregarChavePublica(resourceLoader, localChaveAnterior);
        RSAKey jwkAnterior =
            new RSAKey.Builder(chaveAnterior)
                .keyID(identificadorAnterior)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();
        chaves.add(jwkAnterior);
        chavesPublicas.add(jwkAnterior);
      }

      JWKSet conjunto = new JWKSet(chaves);
      JWKSource<SecurityContext> fonte = (seletor, contexto) -> seletor.select(conjunto);
      return new ConjuntoDeChavesJwt(fonte, new JWKSet(chavesPublicas));
    } catch (Exception exception) {
      if (exception instanceof IllegalStateException illegalStateException) {
        throw illegalStateException;
      }
      throw new IllegalStateException("Não foi possível carregar as chaves RSA do JWT.", exception);
    }
  }

  @Bean
  JwtEncoder jwtEncoder(ConjuntoDeChavesJwt conjunto) {
    return new NimbusJwtEncoder(conjunto.fonte());
  }

  @Bean
  JwtDecoder jwtDecoder(
      ConjuntoDeChavesJwt conjunto, @Value("${quickorder.jwt.issuer}") String emissor) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSource(conjunto.fonte())
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emissor));
    return decoder;
  }

  private static RSAPrivateKey carregarChavePrivada(ResourceLoader resourceLoader, String local)
      throws Exception {
    Resource resource = resourceLoader.getResource(local);
    try (InputStream entrada = resource.getInputStream()) {
      return RsaKeyConverters.pkcs8().convert(entrada);
    }
  }

  private static RSAPublicKey carregarChavePublica(ResourceLoader resourceLoader, String local)
      throws Exception {
    Resource resource = resourceLoader.getResource(local);
    try (InputStream entrada = resource.getInputStream()) {
      return RsaKeyConverters.x509().convert(entrada);
    }
  }

  private static RSAPublicKey derivarChavePublica(RSAPrivateKey chavePrivada) throws Exception {
    if (!(chavePrivada instanceof RSAPrivateCrtKey chaveCrt)) {
      throw new IllegalArgumentException("A chave RSA privada deve conter parâmetros CRT.");
    }
    return (RSAPublicKey)
        KeyFactory.getInstance("RSA")
            .generatePublic(
                new RSAPublicKeySpec(chaveCrt.getModulus(), chaveCrt.getPublicExponent()));
  }

  private static void exigirPreenchido(String valor, String variavel) {
    if (valor.isBlank()) {
      throw new IllegalStateException(variavel + " é obrigatório para emissão e validação de JWT.");
    }
  }
}
