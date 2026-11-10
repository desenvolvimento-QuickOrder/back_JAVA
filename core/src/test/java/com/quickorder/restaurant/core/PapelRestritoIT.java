package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;

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
}
