package com.quickorder.restaurant.core.shared.error;

import java.util.Optional;

@FunctionalInterface
public interface TraducaoDeConstraint {

  Optional<String> traduzir(String nomeDaConstraint);
}
