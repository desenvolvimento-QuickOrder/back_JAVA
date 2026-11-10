package com.quickorder.restaurant.core.shared.web;

import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;

@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

  @Bean
  JacksonModule jsonNullableModule() {
    return new JsonNullableJackson3Module();
  }
}
