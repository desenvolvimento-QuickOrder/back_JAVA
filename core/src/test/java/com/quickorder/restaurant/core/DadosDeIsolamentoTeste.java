package com.quickorder.restaurant.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

record DadosDeIsolamentoTeste(
    UUID restauranteA, UUID restauranteB, UUID registroA, UUID registroB, String valorA) {

  static DadosDeIsolamentoTeste inserirRestaurantesERegistros() throws SQLException {
    UUID restauranteA = UUID.randomUUID();
    UUID restauranteB = UUID.randomUUID();
    UUID registroA = UUID.randomUUID();
    UUID registroB = UUID.randomUUID();
    String valorA = "valor-" + UUID.randomUUID();

    try (Connection connection =
        DriverManager.getConnection(
            IntegracaoBaseIT.POSTGRES.getJdbcUrl(),
            IntegracaoBaseIT.POSTGRES.getUsername(),
            IntegracaoBaseIT.POSTGRES.getPassword())) {
      inserirRestaurante(connection, restauranteA, "restaurante-a");
      inserirRestaurante(connection, restauranteB, "restaurante-b");
      inserirRegistro(connection, registroA, restauranteA, valorA);
      inserirRegistro(connection, registroB, restauranteB, "valor-b-" + UUID.randomUUID());
    }

    return new DadosDeIsolamentoTeste(restauranteA, restauranteB, registroA, registroB, valorA);
  }

  private static void inserirRestaurante(Connection connection, UUID id, String nome)
      throws SQLException {
    String slug = nome + "-" + id.toString().replace("-", "").substring(0, 12);
    try (PreparedStatement statement =
        connection.prepareStatement("insert into restaurante (id, slug, nome) values (?, ?, ?)")) {
      statement.setObject(1, id);
      statement.setString(2, slug);
      statement.setString(3, nome);
      statement.executeUpdate();
    }
  }

  private static void inserirRegistro(
      Connection connection, UUID id, UUID restauranteId, String valor) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "insert into isolamento_teste (id, restaurante_id, valor) values (?, ?, ?)")) {
      statement.setObject(1, id);
      statement.setObject(2, restauranteId);
      statement.setString(3, valor);
      statement.executeUpdate();
    }
  }
}
