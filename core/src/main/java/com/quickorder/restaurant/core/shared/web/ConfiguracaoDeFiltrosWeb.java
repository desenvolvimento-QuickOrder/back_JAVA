package com.quickorder.restaurant.core.shared.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoDeFiltrosWeb {

  @Bean
  FilterRegistrationBean<FiltroDeCorrelationId> registroDoFiltroDeCorrelationId(
      FiltroDeCorrelationId filtro) {
    var registro = new FilterRegistrationBean<>(filtro);
    registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registro;
  }

  @Bean
  FilterRegistrationBean<FiltroDeTenantNoMdc> registroServletDoFiltroDeTenantNoMdc(
      FiltroDeTenantNoMdc filtro) {
    var registro = new FilterRegistrationBean<>(filtro);
    registro.setEnabled(false);
    return registro;
  }
}
