package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import com.quickorder.restaurant.core.shared.tenant.RepositorioDeIsolamentoTeste;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class IsolamentoPorTenantIT extends IntegracaoBaseIT {

  @Autowired private ExecucaoNoTenant execucaoNoTenant;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private RepositorioDeIsolamentoTeste repositorio;

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
  void deveIsolarFindByIdJpqlESqlNativoPorRestaurante() {
    execucaoNoTenant.executar(
        dados.restauranteA(),
        () -> {
          assertThat(repositorio.findById(dados.registroA())).isPresent();
          assertThat(repositorio.findById(dados.registroB())).isEmpty();
          assertThat(repositorio.buscarPorIdComJpql(dados.registroB())).isEmpty();
          assertThat(repositorio.contarPorIdComSqlNativo(dados.registroB())).isZero();
          assertThat(
                  jdbcTemplate.update(
                      "update isolamento_teste set valor = ? where id = ?",
                      "alterado",
                      dados.registroB()))
              .isZero();
          return null;
        });

    assertThatThrownBy(
            () ->
                execucaoNoTenant.executar(
                    dados.restauranteA(),
                    () -> {
                      jdbcTemplate.update(
                          "insert into isolamento_teste (id, restaurante_id, valor) values (?, ?, ?)",
                          UUID.randomUUID(),
                          dados.restauranteB(),
                          "invasão");
                      return null;
                    }))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void deveRetornarZeroLinhasQuandoConsultaSemTenant() {
    Long quantidade =
        new TransactionTemplate(transactionManager)
            .execute(
                status ->
                    jdbcTemplate.queryForObject(
                        "select count(*) from isolamento_teste", Long.class));

    assertThat(quantidade).isZero();
  }

  @Test
  void deveAplicarTenantEmTransacaoSomenteLeitura() {
    Jwt token =
        Jwt.withTokenValue("teste")
            .header("alg", "RS256")
            .claim("tenant_id", dados.restauranteA().toString())
            .build();
    var contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(new JwtAuthenticationToken(token, List.of()));
    SecurityContextHolder.setContext(contexto);

    var transacaoLeitura = new TransactionTemplate(transactionManager);
    transacaoLeitura.setReadOnly(true);
    Long quantidade =
        transacaoLeitura.execute(
            status ->
                jdbcTemplate.queryForObject("select count(*) from isolamento_teste", Long.class));

    assertThat(quantidade).isEqualTo(1);
  }

  @Test
  void devePriorizarTenantDoEscopoSobreClaimDoJwt() {
    Jwt token =
        Jwt.withTokenValue("teste")
            .header("alg", "RS256")
            .claim("tenant_id", dados.restauranteB().toString())
            .build();
    var contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(new JwtAuthenticationToken(token, List.of()));
    SecurityContextHolder.setContext(contexto);

    String tenantDaTransacao =
        execucaoNoTenant.executar(
            dados.restauranteA(),
            () ->
                jdbcTemplate.queryForObject(
                    "select current_setting('app.tenant_id', true)", String.class));

    assertThat(tenantDaTransacao).isEqualTo(dados.restauranteA().toString());
  }

  @Test
  void deveExecutarTenantEmTransacaoNovaDentroDeOutraTransacao() {
    TransactionTemplate transacaoExterna = new TransactionTemplate(transactionManager);
    String resultado =
        transacaoExterna.execute(
            status -> {
              Long idTransacaoExterna =
                  jdbcTemplate.queryForObject("select txid_current()", Long.class);
              String tenantExterno =
                  jdbcTemplate.queryForObject(
                      "select current_setting('app.tenant_id', true)", String.class);
              assertThat(tenantExterno).isNullOrEmpty();

              String resultadoInterno =
                  execucaoNoTenant.executar(
                      dados.restauranteA(),
                      () -> {
                        Long idTransacaoInterna =
                            jdbcTemplate.queryForObject("select txid_current()", Long.class);
                        String tenantInterno =
                            jdbcTemplate.queryForObject(
                                "select current_setting('app.tenant_id', true)", String.class);
                        Long quantidade =
                            jdbcTemplate.queryForObject(
                                "select count(*) from isolamento_teste", Long.class);
                        return idTransacaoInterna + ":" + tenantInterno + ":" + quantidade;
                      });

              Long idTransacaoRetomada =
                  jdbcTemplate.queryForObject("select txid_current()", Long.class);
              assertThat(idTransacaoRetomada).isEqualTo(idTransacaoExterna);
              assertThat(
                      jdbcTemplate.queryForObject(
                          "select count(*) from isolamento_teste", Long.class))
                  .isZero();
              return idTransacaoExterna + ":" + resultadoInterno;
            });

    assertThat(resultado).contains(":" + dados.restauranteA()).endsWith(":1");
  }
}
