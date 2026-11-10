package com.quickorder.restaurant.core.shared.web;

import com.quickorder.restaurant.core.shared.error.ConflitoDeNegocio;
import com.quickorder.restaurant.core.shared.error.ExcecaoDeNegocio;
import com.quickorder.restaurant.core.shared.error.RecursoNaoEncontrado;
import com.quickorder.restaurant.core.shared.error.TraducaoDeConstraint;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ManipuladorGlobalDeErros extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManipuladorGlobalDeErros.class);
  private final List<TraducaoDeConstraint> traducoesDeConstraint;

  public ManipuladorGlobalDeErros(List<TraducaoDeConstraint> traducoesDeConstraint) {
    this.traducoesDeConstraint = traducoesDeConstraint;
  }

  @ExceptionHandler(ExcecaoDeNegocio.class)
  ResponseEntity<Object> tratarExcecaoDeNegocio(ExcecaoDeNegocio exception, WebRequest request) {
    return resposta(
        HttpStatus.UNPROCESSABLE_ENTITY, exception.getCode(), exception.getMessage(), List.of());
  }

  @ExceptionHandler(RecursoNaoEncontrado.class)
  ResponseEntity<Object> tratarRecursoNaoEncontrado(
      RecursoNaoEncontrado exception, WebRequest request) {
    return resposta(
        HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", exception.getMessage(), List.of());
  }

  @ExceptionHandler(ConflitoDeNegocio.class)
  ResponseEntity<Object> tratarConflitoDeNegocio(ConflitoDeNegocio exception, WebRequest request) {
    return resposta(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), List.of());
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ResponseEntity<Object> tratarConflitoDeVersao(
      ObjectOptimisticLockingFailureException exception, WebRequest request) {
    return resposta(
        HttpStatus.CONFLICT,
        "CONFLITO_DE_VERSAO",
        "O recurso foi alterado por outra operação.",
        List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Object> tratarViolacaoDeIntegridade(
      DataIntegrityViolationException exception, WebRequest request) {
    Optional<String> nomeDaConstraint = extrairNomeDaConstraint(exception);
    Optional<String> code =
        nomeDaConstraint.flatMap(
            nome ->
                traducoesDeConstraint.stream()
                    .map(traducao -> traducao.traduzir(nome))
                    .flatMap(Optional::stream)
                    .findFirst());

    if (code.isPresent()) {
      return resposta(
          HttpStatus.CONFLICT,
          code.get(),
          "A operação viola uma restrição de integridade.",
          List.of());
    }

    LOGGER.error(
        "Violação de integridade sem tradução registrada. Constraint={}",
        nomeDaConstraint.orElse("desconhecida"),
        exception);
    return resposta(
        HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Ocorreu um erro interno.", List.of());
  }

  @ExceptionHandler(RuntimeException.class)
  ResponseEntity<Object> tratarErroInesperado(RuntimeException exception, WebRequest request) {
    LOGGER.error("Erro inesperado ao processar a requisição.", exception);
    return resposta(
        HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Ocorreu um erro interno.", List.of());
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    HttpStatus httpStatus = HttpStatus.valueOf(status.value());
    List<Map<String, String>> erros = errosDeCampo(exception);
    ProblemDetail problem =
        problema(
            httpStatus, codeDaRequisicao(exception, status), detalheDaRequisicao(status), erros);
    HttpHeaders cabecalhosDaResposta = new HttpHeaders();
    cabecalhosDaResposta.putAll(headers);
    cabecalhosDaResposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    String correlationId = correlationId(request);
    if (correlationId != null) {
      cabecalhosDaResposta.set(FiltroDeCorrelationId.NOME_DO_HEADER, correlationId);
    }
    return super.handleExceptionInternal(exception, problem, cabecalhosDaResposta, status, request);
  }

  private ResponseEntity<Object> resposta(
      HttpStatus status, String code, String detail, List<Map<String, String>> erros) {
    ProblemDetail problem = problema(status, code, detail, erros);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private ProblemDetail problema(
      HttpStatus status, String code, String detail, List<Map<String, String>> erros) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(titulo(status));
    problem.setProperty("code", code);

    String correlationId = MDC.get(FiltroDeCorrelationId.CHAVE_MDC);
    if (correlationId != null) {
      problem.setProperty("correlationId", correlationId);
    }
    if (!erros.isEmpty()) {
      problem.setProperty("errors", erros);
    }
    return problem;
  }

  private List<Map<String, String>> errosDeCampo(Exception exception) {
    if (!(exception instanceof MethodArgumentNotValidException invalida)) {
      return List.of();
    }
    List<Map<String, String>> erros = new ArrayList<>();
    for (FieldError erro : invalida.getBindingResult().getFieldErrors()) {
      erros.add(
          Map.of(
              "campo",
              erro.getField(),
              "mensagem",
              Optional.ofNullable(erro.getDefaultMessage()).orElse("Valor inválido.")));
    }
    return erros;
  }

  private Optional<String> extrairNomeDaConstraint(Throwable causaRaiz) {
    Deque<Throwable> causas = new ArrayDeque<>();
    Set<Throwable> visitadas = new HashSet<>();
    causas.add(causaRaiz);
    while (!causas.isEmpty()) {
      Throwable causa = causas.removeFirst();
      if (!visitadas.add(causa)) {
        continue;
      }
      if (causa instanceof PSQLException excecaoPostgres
          && excecaoPostgres.getServerErrorMessage() != null) {
        String nome = excecaoPostgres.getServerErrorMessage().getConstraint();
        if (nome != null && !nome.isBlank()) {
          return Optional.of(nome);
        }
      }
      if (causa.getCause() != null) {
        causas.addLast(causa.getCause());
      }
      if (causa instanceof SQLException excecaoSql && excecaoSql.getNextException() != null) {
        causas.addLast(excecaoSql.getNextException());
      }
    }
    return Optional.empty();
  }

  private String codeDaRequisicao(Exception exception, HttpStatusCode status) {
    if (exception instanceof HttpRequestMethodNotSupportedException || status.value() == 405) {
      return "METODO_NAO_SUPORTADO";
    }
    if (exception instanceof HttpMediaTypeNotSupportedException
        || exception instanceof HttpMediaTypeNotAcceptableException
        || status.value() == 415) {
      return "MIDIA_NAO_SUPORTADA";
    }
    if (exception instanceof NoHandlerFoundException
        || exception instanceof NoResourceFoundException
        || status.value() == 404) {
      return "RECURSO_NAO_ENCONTRADO";
    }
    if (exception instanceof TypeMismatchException
        || exception instanceof HttpMessageNotReadableException
        || status.value() == 400) {
      return "REQUISICAO_INVALIDA";
    }
    if (exception instanceof MethodArgumentNotValidException
        || exception instanceof HandlerMethodValidationException) {
      return "VALIDACAO_INVALIDA";
    }
    if (status.value() == 429) {
      return "LIMITE_EXCEDIDO";
    }
    return "ERRO_INTERNO";
  }

  private String detalheDaRequisicao(HttpStatusCode status) {
    return switch (status.value()) {
      case 400 -> "A requisição contém dados inválidos.";
      case 404 -> "Recurso não encontrado.";
      case 405 -> "Método não suportado para este recurso.";
      case 406, 415 -> "Formato de mídia não suportado.";
      case 429 -> "Limite de requisições excedido.";
      default -> "Não foi possível processar a requisição.";
    };
  }

  private String correlationId(WebRequest request) {
    String correlationId = MDC.get(FiltroDeCorrelationId.CHAVE_MDC);
    if (correlationId != null) {
      return correlationId;
    }
    if (!(request instanceof NativeWebRequest nativeRequest)) {
      return null;
    }
    HttpServletRequest servletRequest = nativeRequest.getNativeRequest(HttpServletRequest.class);
    if (servletRequest == null) {
      return null;
    }
    Object atributo = servletRequest.getAttribute(FiltroDeCorrelationId.CHAVE_MDC);
    return atributo instanceof String valor ? valor : null;
  }

  private String titulo(HttpStatus status) {
    return switch (status) {
      case BAD_REQUEST -> "Requisição inválida";
      case NOT_FOUND -> "Recurso não encontrado";
      case METHOD_NOT_ALLOWED -> "Método não suportado";
      case NOT_ACCEPTABLE, UNSUPPORTED_MEDIA_TYPE -> "Mídia não suportada";
      case UNPROCESSABLE_ENTITY -> "Regra de negócio não atendida";
      case CONFLICT -> "Conflito";
      case TOO_MANY_REQUESTS -> "Limite excedido";
      case INTERNAL_SERVER_ERROR -> "Erro interno";
      default -> status.getReasonPhrase();
    };
  }
}
