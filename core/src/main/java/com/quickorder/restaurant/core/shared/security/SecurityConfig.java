package com.quickorder.restaurant.core.shared.security;

import com.quickorder.restaurant.core.shared.web.FiltroDeTenantNoMdc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter(ConversorDePapeisJwt conversor) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(conversor);
    return converter;
  }

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler,
      FiltroDeTenantNoMdc filtroDeTenantNoMdc)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/cardapio/{slug}")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET, "/api/v1/usuarios", "/api/v1/usuarios/{usuarioId}")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/usuarios")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/usuarios/{usuarioId}")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/{usuarioId}/ativar")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/{usuarioId}/desativar")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/mesas", "/api/v1/mesas/{mesaId}")
                    .hasAnyRole("ADMIN", "GARCOM", "COPA")
                    .requestMatchers(HttpMethod.POST, "/api/v1/mesas")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/mesas/{mesaId}")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.GET, "/api/v1/categorias", "/api/v1/categorias/{categoriaId}")
                    .hasAnyRole("ADMIN", "GARCOM", "COPA")
                    .requestMatchers(HttpMethod.POST, "/api/v1/categorias")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/categorias/{categoriaId}")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.GET, "/api/v1/produtos", "/api/v1/produtos/{produtoId}")
                    .hasAnyRole("ADMIN", "GARCOM", "COPA")
                    .requestMatchers(HttpMethod.POST, "/api/v1/produtos")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/v1/produtos/{produtoId}",
                        "/api/v1/produtos/{produtoId}/disponibilidade")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.GET, "/api/v1/comandas", "/api/v1/comandas/{comandaId}")
                    .hasAnyRole("ADMIN", "GARCOM")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/comandas",
                        "/api/v1/comandas/{comandaId}/fechar",
                        "/api/v1/comandas/{comandaId}/transferir-mesa")
                    .hasAnyRole("ADMIN", "GARCOM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/comandas/{comandaId}/pedidos")
                    .hasAnyRole("ADMIN", "GARCOM")
                    .requestMatchers(
                        HttpMethod.GET, "/api/v1/pedidos", "/api/v1/pedidos/{pedidoId}")
                    .hasAnyRole("ADMIN", "GARCOM", "COPA")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/pedidos/{pedidoId}/iniciar-preparo",
                        "/api/v1/pedidos/{pedidoId}/marcar-pronto")
                    .hasRole("COPA")
                    .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/{pedidoId}/entregar")
                    .hasRole("GARCOM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/{pedidoId}/cancelar")
                    .hasAnyRole("ADMIN", "GARCOM")
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .addFilterAfter(filtroDeTenantNoMdc, BearerTokenAuthenticationFilter.class)
        .build();
  }
}
