package com.quickorder.restaurant.core.equipe.web;

import com.quickorder.restaurant.core.equipe.domain.CredenciaisInvalidas;
import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativas;
import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativasExcedido;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import com.quickorder.restaurant.core.equipe.domain.SessaoInvalida;
import com.quickorder.restaurant.core.shared.web.ClientIpResolver;
import com.quickorder.restaurant.core.shared.web.FiltroDeCorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManipuladorDeErrosDeAutenticacao {

  private final LimiteDeTentativas limiteDeTentativas;
  private final Relogio relogio;
  private final ClientIpResolver clientIpResolver;

  public ManipuladorDeErrosDeAutenticacao(
      LimiteDeTentativas limiteDeTentativas, Relogio relogio, ClientIpResolver clientIpResolver) {
    this.limiteDeTentativas = limiteDeTentativas;
    this.relogio = relogio;
    this.clientIpResolver = clientIpResolver;
  }

  @ExceptionHandler(CredenciaisInvalidas.class)
  ResponseEntity<ProblemDetail> credenciaisInvalidas() {
    return problema(
        HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "Credenciais inválidas.", null);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<ProblemDetail> cabecalhoDeDispositivoAusente(HttpServletRequest request) {
    long retryAfter =
        limiteDeTentativas.reservarTentativaPorIp(
            clientIpResolver.resolver(request), relogio.agora());
    if (retryAfter > 0) {
      return problema(
          HttpStatus.TOO_MANY_REQUESTS,
          "LIMITE_EXCEDIDO",
          "Limite de tentativas excedido.",
          retryAfter);
    }
    return problema(
        HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "Credenciais inválidas.", null);
  }

  @ExceptionHandler(SessaoInvalida.class)
  ResponseEntity<ProblemDetail> sessaoInvalida() {
    return problema(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Sessão inválida.", null);
  }

  @ExceptionHandler(LimiteDeTentativasExcedido.class)
  ResponseEntity<ProblemDetail> limiteExcedido(LimiteDeTentativasExcedido excecao) {
    return problema(
        HttpStatus.TOO_MANY_REQUESTS,
        "LIMITE_EXCEDIDO",
        "Limite de tentativas excedido.",
        excecao.retryAfterSeconds());
  }

  private ResponseEntity<ProblemDetail> problema(
      HttpStatus status, String code, String detalhe, Long retryAfterSeconds) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
    problem.setTitle(status == HttpStatus.UNAUTHORIZED ? "Não autenticado" : "Limite excedido");
    problem.setProperty("code", code);
    String correlationId = MDC.get(FiltroDeCorrelationId.CHAVE_MDC);
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
    }

    ResponseEntity.BodyBuilder resposta =
        ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON);
    if (retryAfterSeconds != null) {
      resposta.header(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString());
    }
    return resposta.body(problem);
  }
}
