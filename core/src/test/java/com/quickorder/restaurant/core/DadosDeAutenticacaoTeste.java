package com.quickorder.restaurant.core;

import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

record DadosDeAutenticacaoTeste(
    UUID restauranteId, UUID usuarioId, String slug, String login, String senha) {

  static DadosDeAutenticacaoTeste criar(
      RepositorioDeUsuarios usuarios,
      HashDeSenha hashDeSenha,
      ExecucaoNoTenant execucaoNoTenant,
      Papel papel,
      boolean usuarioAtivo,
      boolean restauranteAtivo)
      throws SQLException {
    UUID restauranteId = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();
    String slug = "restaurante-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    String login = "admin";
    String senha = "senha-forte-de-teste-123";
    inserirRestaurante(restauranteId, slug, restauranteAtivo);

    Usuario usuario =
        new Usuario(
            usuarioId, restauranteId, login, login, hashDeSenha.gerar(senha), papel, usuarioAtivo);
    execucaoNoTenant.executar(restauranteId, () -> usuarios.salvar(usuario));
    return new DadosDeAutenticacaoTeste(restauranteId, usuarioId, slug, login, senha);
  }

  static void definirRestauranteAtivo(UUID restauranteId, boolean ativo) throws SQLException {
    try (Connection conexao = conexaoDona();
        PreparedStatement comando =
            conexao.prepareStatement("update restaurante set ativo = ? where id = ?")) {
      comando.setBoolean(1, ativo);
      comando.setObject(2, restauranteId);
      comando.executeUpdate();
    }
  }

  private static void inserirRestaurante(UUID id, String slug, boolean ativo) throws SQLException {
    try (Connection conexao = conexaoDona();
        PreparedStatement comando =
            conexao.prepareStatement(
                "insert into restaurante (id, slug, nome, ativo) values (?, ?, ?, ?)")) {
      comando.setObject(1, id);
      comando.setString(2, slug);
      comando.setString(3, slug);
      comando.setBoolean(4, ativo);
      comando.executeUpdate();
    }
  }

  private static Connection conexaoDona() throws SQLException {
    return DriverManager.getConnection(
        IntegracaoBaseIT.POSTGRES.getJdbcUrl(),
        IntegracaoBaseIT.POSTGRES.getUsername(),
        IntegracaoBaseIT.POSTGRES.getPassword());
  }
}
