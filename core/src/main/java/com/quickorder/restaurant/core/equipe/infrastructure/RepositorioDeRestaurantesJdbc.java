package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.RepositorioDeRestaurantes;
import com.quickorder.restaurant.core.equipe.domain.Restaurante;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeRestaurantesJdbc implements RepositorioDeRestaurantes {

  private final JdbcTemplate jdbcTemplate;

  RepositorioDeRestaurantesJdbc(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<Restaurante> buscarPorSlug(String slug) {
    return jdbcTemplate
        .query(
            "select id, slug, ativo from restaurante where slug = ?",
            (resultado, numero) ->
                new Restaurante(
                    resultado.getObject("id", UUID.class),
                    resultado.getString("slug"),
                    resultado.getBoolean("ativo")),
            slug)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<Restaurante> buscarPorId(UUID id) {
    return jdbcTemplate
        .query(
            "select id, slug, ativo from restaurante where id = ?",
            (resultado, numero) ->
                new Restaurante(
                    resultado.getObject("id", UUID.class),
                    resultado.getString("slug"),
                    resultado.getBoolean("ativo")),
            id)
        .stream()
        .findFirst();
  }
}
