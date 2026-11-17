package com.quickorder.restaurant.core;

import com.quickorder.restaurant.core.shared.error.TraducaoDeConstraint;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@TestConfiguration(proxyBeanMethods = false)
class ConfiguracaoWebTeste {

  @Bean
  @Order(1)
  SecurityFilterChain securityFilterChainDeTeste(
      HttpSecurity http,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler)
      throws Exception {
    return http.securityMatcher("/teste/**")
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            configuracao -> configuracao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            configuracao ->
                configuracao
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            autorizacao ->
                autorizacao
                    .requestMatchers("/teste/erros/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/teste/seguranca/protegido")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .denyAll())
        .build();
  }

  @Bean
  TraducaoDeConstraint traducaoDaConstraintDeTeste() {
    return nome ->
        "isolamento_teste_pkey".equals(nome) ? Optional.of("REGISTRO_DUPLICADO") : Optional.empty();
  }
}
