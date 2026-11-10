package com.quickorder.restaurant.core.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroDeTenantNoMdc extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao instanceof JwtAuthenticationToken token) {
      String tenantId = token.getToken().getClaimAsString("tenant_id");
      if (tenantId != null && !tenantId.isBlank()) {
        MDC.put("tenant_id", tenantId);
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("tenant_id");
    }
  }
}
