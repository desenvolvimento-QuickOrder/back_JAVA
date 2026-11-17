package com.quickorder.restaurant.core.equipe.domain;

import java.util.List;

public record PaginaDeDispositivos(List<Dispositivo> items, int page, int size, long total) {}
