package com.quickorder.restaurant.core.shared.error;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getAllErrors().stream()
            .filter(FieldError.class::isInstance)
            .map(FieldError.class::cast)
            .findFirst()
            .map(fieldError -> fieldError.getDefaultMessage())
            .orElse("VALIDACAO_INVALIDA");

    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setTitle("Validação inválida");
    problem.setType(URI.create("https://quickorder.local/errors/VALIDACAO_INVALIDA"));
    problem.setProperty("code", "VALIDACAO_INVALIDA");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Requisição inválida");
    problem.setType(URI.create("https://quickorder.local/errors/VALIDACAO_INVALIDA"));
    problem.setProperty("code", "VALIDACAO_INVALIDA");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }
}
