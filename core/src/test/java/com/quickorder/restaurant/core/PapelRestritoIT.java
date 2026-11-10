package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PapelRestritoIT extends IntegracaoBaseIT {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void aplicaComOPapelRestrito() {
    String usuarioAtual = jdbcTemplate.queryForObject("select current_user", String.class);

    assertThat(usuarioAtual).isEqualTo("quickorder_app");
  }

  @Test
  void papelDaAplicacaoNaoPodeSerSuperusuarioNemIgnorarRls() {
    var papel =
        jdbcTemplate.queryForMap(
            "select rolsuper, rolbypassrls from pg_roles where rolname = current_user");

    assertThat(papel.get("rolsuper")).isEqualTo(false);
    assertThat(papel.get("rolbypassrls")).isEqualTo(false);
  }

  @Test
  void todaTabelaPublicaDeveTerRlsOuEstarNaListaDeExcecoes() {
    verificarRlsDasTabelasPublicas();
  }

  @Test
  void deveDetectarTabelaPublicaNovaSemRlsForaDasExcecoes() throws Exception {
    String nomeTabela = "tabela_sem_rls_" + UUID.randomUUID().toString().replace("-", "");

    try (Connection connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement statement = connection.createStatement()) {
      statement.execute("create table public." + nomeTabela + " (id uuid)");
      try {
        assertThatThrownBy(this::verificarRlsDasTabelasPublicas).isInstanceOf(AssertionError.class);
      } finally {
        statement.execute("drop table public." + nomeTabela);
      }
    }
  }

  private List<String> tabelasPublicasSemRls() {
    return jdbcTemplate.queryForList(
        """
        select c.relname
        from pg_class c
        join pg_namespace n on n.oid = c.relnamespace
        where n.nspname = 'public'
          and c.relkind in ('r', 'p')
          and not c.relrowsecurity
          and c.relname not in (
            'restaurante',
            'refresh_token',
            'outbox',
            'idempotency_key',
            'flyway_schema_history'
          )
        order by c.relname
        """,
        String.class);
  }

  private void verificarRlsDasTabelasPublicas() {
    assertThat(tabelasPublicasSemRls()).isEmpty();
  }
}
