package com.quickorder.restaurant.core.mesa.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

@JsonIgnoreProperties(ignoreUnknown = false)
public record MesaUpdateRequest(
    JsonNullable<@Size(max = 20, message = "NUMERO_INVALIDO") String> numero,
    JsonNullable<
            @Min(value = 1, message = "CAPACIDADE_INVALIDA")
            @jakarta.validation.constraints.Max(value = 100, message = "CAPACIDADE_INVALIDA")
            Integer>
        capacidade,
    JsonNullable<Boolean> ativa) {

  @JsonAnySetter
  public void campoDesconhecido(String nome, Object valor) {
    throw new IllegalArgumentException("CAMPO_DESCONHECIDO");
  }

  public boolean vazio() {
    return (numero == null || !numero.isPresent())
        && (capacidade == null || !capacidade.isPresent())
        && (ativa == null || !ativa.isPresent());
  }
}
