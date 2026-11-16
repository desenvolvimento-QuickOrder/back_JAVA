package com.quickorder.restaurant.core.mesa.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MesaCreateRequest(
    @NotBlank(message = "NUMERO_OBRIGATORIO") @Size(max = 20, message = "NUMERO_INVALIDO")
        String numero,
    @NotNull(message = "CAPACIDADE_OBRIGATORIA") @Min(value = 1, message = "CAPACIDADE_INVALIDA")
        Integer capacidade) {}
