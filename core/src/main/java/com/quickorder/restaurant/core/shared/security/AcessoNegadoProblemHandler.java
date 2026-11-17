package com.quickorder.restaurant.core.shared.security;

import com.quickorder.restaurant.core.shared.web.EscritorDeProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class AcessoNegadoProblemHandler implements AccessDeniedHandler {

  private final EscritorDeProblem escritorDeProblem;

  public AcessoNegadoProblemHandler(EscritorDeProblem escritorDeProblem) {
    this.escritorDeProblem = escritorDeProblem;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    escritorDeProblem.escrever(
        request, response, HttpStatus.FORBIDDEN, "SEM_PERMISSAO", "Acesso não permitido.");
  }
}
