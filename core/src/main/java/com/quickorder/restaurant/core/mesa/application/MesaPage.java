package com.quickorder.restaurant.core.mesa.application;

import java.util.List;

public record MesaPage<T>(int page, int size, long totalElements, int totalPages, List<T> items) {
  public static <T> MesaPage<T> from(List<T> items, int page, int size, long totalElements) {
    int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalElements / size);
    return new MesaPage<>(page, size, totalElements, Math.max(totalPages, 1), items);
  }
}
