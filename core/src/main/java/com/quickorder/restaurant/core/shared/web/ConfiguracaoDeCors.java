package com.quickorder.restaurant.core.shared.web;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoDeCors {

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      Environment environment, @Value("${quickorder.cors.origens:}") String origens) {
    List<String> origensPermitidas =
        Arrays.stream(origens.split(","))
            .map(String::trim)
            .filter(origem -> !origem.isEmpty())
            .toList();

    if (origensPermitidas.isEmpty() && !environment.acceptsProfiles(Profiles.of("dev", "test"))) {
      throw new IllegalStateException(
          "QUICKORDER_CORS_ORIGINS precisa conter ao menos uma origem fora dos perfis dev e test.");
    }

    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(origensPermitidas);
    cors.setAllowCredentials(false);
    cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "X-Correlation-Id",
            "Idempotency-Key",
            "X-Device-Token"));
    cors.setExposedHeaders(List.of("X-Correlation-Id", "Retry-After", "Location"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }
}
