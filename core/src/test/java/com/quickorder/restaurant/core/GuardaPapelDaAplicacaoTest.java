package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.shared.tenant.GuardaPapelDaAplicacao;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class GuardaPapelDaAplicacaoTest {

  @Test
  void deveFalharNaSubidaQuandoPapelTemBypassRls() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    when(jdbcTemplate.queryForMap(
            "select rolsuper, rolbypassrls from pg_roles where rolname = current_user"))
        .thenReturn(Map.of("rolsuper", false, "rolbypassrls", true));

    GuardaPapelDaAplicacao guarda = new GuardaPapelDaAplicacao(jdbcTemplate);

    assertThatThrownBy(() -> guarda.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("BYPASSRLS");
  }
}
