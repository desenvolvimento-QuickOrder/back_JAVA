package com.quickorder.restaurant.core.mesa.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record MesaUpdateRequest(
    @Size(max = 20, message = "NUMERO_INVALIDO") String numero,
    @Min(value = 1, message = "CAPACIDADE_INVALIDA") Integer capacidade,
    Boolean ativa) {}
