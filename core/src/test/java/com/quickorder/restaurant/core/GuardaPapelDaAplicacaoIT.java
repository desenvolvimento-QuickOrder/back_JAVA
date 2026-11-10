package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class GuardaPapelDaAplicacaoIT extends IntegracaoBaseIT {

  @Test
  void deveFalharNaSubidaQuandoPapelConectadoTemBypassRls() throws Exception {
    String papel = "quickorder_bypass_" + UUID.randomUUID().toString().replace("-", "");
    criarPapelComBypassRls(papel);
    ConfigurableApplicationContext contexto = null;
    try {
      SpringApplication aplicacao = new SpringApplication(QuickOrderApplication.class);
      aplicacao.setAdditionalProfiles("test");

      try {
        contexto =
            aplicacao.run(
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + papel,
                "--spring.datasource.password=senha-teste",
                "--spring.flyway.enabled=false",
                "--spring.datasource.hikari.maximum-pool-size=1",
                "--quickorder.cors.origens=http://localhost:3000",
                "--server.port=0");
      } catch (RuntimeException falha) {
        assertThat(falha)
            .hasStackTraceContaining(
                "O papel da aplicação não pode ser superusuário nem possuir BYPASSRLS.");
        return;
      }
      throw new AssertionError("A aplicação iniciou com um papel que possui BYPASSRLS.");
    } finally {
      if (contexto != null) {
        contexto.close();
      }
      removerPapel(papel);
    }
  }

  private void criarPapelComBypassRls(String papel) throws Exception {
    try (Connection connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement statement = connection.createStatement()) {
      statement.execute("create role " + papel + " login bypassrls password 'senha-teste' inherit");
      statement.execute("grant quickorder_app to " + papel);
    }
  }

  private void removerPapel(String papel) throws Exception {
    try (Connection connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement statement = connection.createStatement()) {
      statement.execute("drop role if exists " + papel);
    }
  }
}
