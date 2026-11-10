package com.quickorder.restaurant.core.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class EscritorDeProblem {

  private final ObjectMapper objectMapper;

  public EscritorDeProblem(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void escrever(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String detail)
      throws IOException {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(titulo(status));
    problem.setProperty("code", code);

    String correlationId = MDC.get(FiltroDeCorrelationId.CHAVE_MDC);
    if (correlationId == null) {
      Object atributo = request.getAttribute(FiltroDeCorrelationId.CHAVE_MDC);
      correlationId = atributo instanceof String valor ? valor : null;
    }
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
      response.setHeader(FiltroDeCorrelationId.NOME_DO_HEADER, correlationId);
    }

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getOutputStream(), problem);
  }

  private String titulo(HttpStatus status) {
    return switch (status) {
      case UNAUTHORIZED -> "Não autenticado";
      case FORBIDDEN -> "Sem permissão";
      case INTERNAL_SERVER_ERROR -> "Erro interno";
      default -> status.getReasonPhrase();
    };
  }
}
