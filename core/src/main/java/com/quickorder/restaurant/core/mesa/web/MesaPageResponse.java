package com.quickorder.restaurant.core.mesa.web;

import java.util.List;

public record MesaPageResponse<T>(
    int page,
    int size,
    long totalElements,
    int totalPages,
    List<T> items) {}
