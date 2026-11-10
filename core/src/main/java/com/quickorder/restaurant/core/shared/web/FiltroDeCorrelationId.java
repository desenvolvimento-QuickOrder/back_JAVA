package com.quickorder.restaurant.core.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroDeCorrelationId extends OncePerRequestFilter {

  public static final String NOME_DO_HEADER = "X-Correlation-Id";
  public static final String CHAVE_MDC = "correlationId";
  private static final Pattern FORMATO_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String recebido = request.getHeader(NOME_DO_HEADER);
    String correlationId =
        recebido != null && FORMATO_VALIDO.matcher(recebido).matches()
            ? recebido
            : UUID.randomUUID().toString();

    MDC.put(CHAVE_MDC, correlationId);
    request.setAttribute(CHAVE_MDC, correlationId);
    response.setHeader(NOME_DO_HEADER, correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CHAVE_MDC);
    }
  }
}
