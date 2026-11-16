package com.quickorder.restaurant.core.mesa.web;

import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import java.util.UUID;

public record MesaResponse(
    UUID id, String numero, Integer capacidade, MesaStatus status, Boolean ativa) {}
