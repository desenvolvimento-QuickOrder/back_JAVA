package com.quickorder.restaurant.core.shared.error;

import com.quickorder.restaurant.core.mesa.domain.MesaDuplicadoException;
import com.quickorder.restaurant.core.mesa.domain.MesaNotFoundException;
import com.quickorder.restaurant.core.mesa.domain.MesaOcupadaException;
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

  @ExceptionHandler(MesaNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleMesaNotFound(MesaNotFoundException ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Mesa não encontrada");
    problem.setType(URI.create("https://quickorder.local/errors/MESA_NAO_ENCONTRADA"));
    problem.setProperty("code", "MESA_NAO_ENCONTRADA");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(MesaDuplicadoException.class)
  public ResponseEntity<ProblemDetail> handleMesaDuplicada(MesaDuplicadoException ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Número de mesa duplicado");
    problem.setType(URI.create("https://quickorder.local/errors/MESA_NUMERO_DUPLICADO"));
    problem.setProperty("code", "MESA_NUMERO_DUPLICADO");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(MesaOcupadaException.class)
  public ResponseEntity<ProblemDetail> handleMesaOcupada(MesaOcupadaException ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    problem.setTitle("Regra de negócio violada");
    problem.setType(URI.create("https://quickorder.local/errors/MESA_OCUPADA"));
    problem.setProperty("code", "MESA_OCUPADA");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    String detail = ex.getBindingResult().getAllErrors().stream()
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

  @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
  public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Requisição inválida");
    problem.setType(URI.create("https://quickorder.local/errors/VALIDACAO_INVALIDA"));
    problem.setProperty("code", "VALIDACAO_INVALIDA");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }
}
