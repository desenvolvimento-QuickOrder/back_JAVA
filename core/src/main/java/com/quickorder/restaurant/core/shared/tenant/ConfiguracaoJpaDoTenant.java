package com.quickorder.restaurant.core.shared.tenant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoJpaDoTenant {

  @Bean
  JpaVendorAdapter jpaVendorAdapter(TenantContext tenantContext) {
    return new HibernateJpaVendorAdapter() {
      @Override
      public HibernateJpaDialect getJpaDialect() {
        return new DialetoJpaDoTenant(tenantContext);
      }
    };
  }
}
