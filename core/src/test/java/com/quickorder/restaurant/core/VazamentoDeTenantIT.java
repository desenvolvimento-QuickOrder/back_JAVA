package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class VazamentoDeTenantIT extends IntegracaoBaseIT {

  @DynamicPropertySource
  static void limitarPool(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.hikari.maximum-pool-size", () -> 1);
  }

  @Autowired private ExecucaoNoTenant execucaoNoTenant;
  @Autowired private JdbcTemplate jdbcTemplate;

  private DadosDeIsolamentoTeste dados;

  @BeforeEach
  void prepararRestaurantes() throws Exception {
    SecurityContextHolder.clearContext();
    dados = DadosDeIsolamentoTeste.inserirRestaurantesERegistros();
  }

  @AfterEach
  void limparContextoDeSeguranca() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void naoDeveHerdarTenantEntreTransacoesNaMesmaConexao() {
    execucaoNoTenant.executar(
        dados.restauranteA(),
        () -> jdbcTemplate.queryForObject("select count(*) from isolamento_teste", Long.class));

    Long quantidadeSemTenant =
        jdbcTemplate.queryForObject("select count(*) from isolamento_teste", Long.class);
    String tenantDaConexao =
        jdbcTemplate.queryForObject("select current_setting('app.tenant_id', true)", String.class);

    assertThat(quantidadeSemTenant).isZero();
    assertThat(tenantDaConexao).isNullOrEmpty();
  }
}
