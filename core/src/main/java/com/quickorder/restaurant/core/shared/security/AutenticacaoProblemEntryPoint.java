package com.quickorder.restaurant.core.shared.security;

import com.quickorder.restaurant.core.shared.web.EscritorDeProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class AutenticacaoProblemEntryPoint implements AuthenticationEntryPoint {

  private final EscritorDeProblem escritorDeProblem;

  public AutenticacaoProblemEntryPoint(EscritorDeProblem escritorDeProblem) {
    this.escritorDeProblem = escritorDeProblem;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    escritorDeProblem.escrever(
        request, response, HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Autenticação necessária.");
  }
}
