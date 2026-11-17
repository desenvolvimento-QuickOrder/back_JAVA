package com.quickorder.restaurant.core.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class FiltroDeTrocaDePin extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;

  public FiltroDeTrocaDePin(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao instanceof JwtAuthenticationToken jwt
        && Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean("troca_pin_pendente"))
        && !(request.getMethod().equals("POST")
            && request.getRequestURI().equals("/api/v1/auth/trocar-pin"))) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/problem+json");
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      objectMapper.writeValue(
          response.getOutputStream(),
          new Problema(
              "about:blank",
              "Troca de PIN obrigatória",
              403,
              "É necessário trocar o PIN antes de continuar.",
              "TROCA_DE_PIN_OBRIGATORIA",
              MDC.get("correlationId")));
      return;
    }
    filterChain.doFilter(request, response);
  }

  private record Problema(
      String type, String title, int status, String detail, String code, String correlationId) {}
}
