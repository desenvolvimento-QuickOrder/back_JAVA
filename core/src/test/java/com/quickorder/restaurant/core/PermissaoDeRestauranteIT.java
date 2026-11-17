package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PermissaoDeRestauranteIT extends IntegracaoBaseIT {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void papelDaAplicacaoSoPodeLerORestaurante() {
    assertThat(
            jdbcTemplate.queryForObject(
                "select has_table_privilege(current_user, 'restaurante', 'SELECT')", Boolean.class))
        .isTrue();
    assertThat(
            jdbcTemplate.queryForObject(
                "select has_table_privilege(current_user, 'restaurante', 'INSERT')", Boolean.class))
        .isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "select has_table_privilege(current_user, 'restaurante', 'UPDATE')", Boolean.class))
        .isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "select has_table_privilege(current_user, 'restaurante', 'DELETE')", Boolean.class))
        .isFalse();
  }
}
