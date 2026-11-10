package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HashDeSenhaBcryptHmac implements HashDeSenha {

  private static final String ALGORITMO = "HmacSHA256";
  private static final String IDENTIFICADOR = "bcrypt-hmac-v1";

  private final byte[] pepper;
  private final PasswordEncoder codificador;

  public HashDeSenhaBcryptHmac(
      @Value("${quickorder.senha.pepper:}") String pepper, Environment environment) {
    if (pepper.isBlank() && !environment.acceptsProfiles(Profiles.of("dev", "test"))) {
      throw new IllegalStateException(
          "QUICKORDER_SENHA_PEPPER é obrigatório fora dos perfis dev e test.");
    }
    if (pepper.isBlank()) {
      throw new IllegalStateException("O pepper de senha não pode ser vazio.");
    }
    this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
    codificador =
        new DelegatingPasswordEncoder(
            IDENTIFICADOR, Map.of(IDENTIFICADOR, new BCryptPasswordEncoder()));
  }

  @Override
  public String gerar(String senha) {
    return codificador.encode(hmac(senha));
  }

  @Override
  public boolean corresponde(String senha, String hash) {
    if (hash == null || !hash.startsWith("{" + IDENTIFICADOR + "}")) {
      return false;
    }
    return codificador.matches(hmac(senha), hash);
  }

  private String hmac(String senha) {
    try {
      Mac mac = Mac.getInstance(ALGORITMO);
      mac.init(new SecretKeySpec(pepper, ALGORITMO));
      return Base64.getEncoder()
          .encodeToString(mac.doFinal(senha.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("Não foi possível calcular o hash da senha.", exception);
    }
  }
}
