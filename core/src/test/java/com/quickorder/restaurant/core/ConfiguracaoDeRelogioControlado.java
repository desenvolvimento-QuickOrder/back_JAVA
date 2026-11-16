package com.quickorder.restaurant.core;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracaoDeRelogioControlado {

  @Bean
  @Primary
  RelogioControlado relogioControlado() {
    return new RelogioControlado();
  }
}
