package com.quickorder.restaurant.core.shared.tenant;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "quickorder.seguranca.guarda-papel",
    name = "habilitada",
    havingValue = "true",
    matchIfMissing = true)
public class GuardaPapelDaAplicacao implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  public GuardaPapelDaAplicacao(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    Map<String, Object> atributos =
        jdbcTemplate.queryForMap(
            "select rolsuper, rolbypassrls from pg_roles where rolname = current_user");
    if (Boolean.TRUE.equals(atributos.get("rolsuper"))
        || Boolean.TRUE.equals(atributos.get("rolbypassrls"))) {
      throw new IllegalStateException(
          "O papel da aplicação não pode ser superusuário nem possuir BYPASSRLS.");
    }
  }
}
