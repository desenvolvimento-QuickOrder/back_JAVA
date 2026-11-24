package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
class MesasIT extends IntegracaoBaseIT {

  private static final AtomicInteger SEQUENCIA_IP = new AtomicInteger(1);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private RepositorioDeUsuarios usuarios;
  @Autowired private HashDeSenha hashDeSenha;
  @Autowired private ExecucaoNoTenant execucaoNoTenant;
  private DadosDeAutenticacaoTeste tenantAtual;
  private Papel papelAtual;

  @Test
  void adminCriaMesaComRespostaCompleta() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);

    MvcResult resultado =
        executar(
                post("/api/v1/mesas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"numero\":\"12\",\"capacidade\":4}"))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andReturn();

    JsonNode corpo = json(resultado);
    assertThat(corpo.get("numero").asString()).isEqualTo("12");
    assertThat(corpo.get("status").asString()).isEqualTo("LIVRE");
    assertThat(corpo.get("ativa").asBoolean()).isTrue();
    assertThat(resultado.getResponse().getContentType())
        .startsWith(MediaType.APPLICATION_JSON_VALUE);
    assertThat(admin.restauranteId()).isNotNull();
  }

  @Test
  void restaurantesDiferentesAceitamMesmoNumero() throws Exception {
    criarMesa(criarUsuario(Papel.ADMIN), "12", 4);
    var outro = criarUsuario(Papel.ADMIN);

    executar(
            post("/api/v1/mesas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"12\",\"capacidade\":4}"))
        .andExpect(status().isCreated());

    assertThat(outro.restauranteId()).isNotNull();
  }

  @Test
  void numeroDuplicadoNoMesmoRestauranteRetornaConflito() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    criarMesa(admin, "12", 4);

    MvcResult resultado =
        executar(
                post("/api/v1/mesas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"numero\":\"12\",\"capacidade\":4}"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andReturn();

    assertThat(json(resultado).get("code").asString()).isEqualTo("MESA_NUMERO_DUPLICADO");
  }

  @Test
  void postRejeitaCapacidadeENumeroInvalidos() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    for (String corpo :
        List.of(
            "{\"numero\":\"1\",\"capacidade\":0}",
            "{\"numero\":\"1\",\"capacidade\":101}",
            "{\"numero\":\"\",\"capacidade\":4}",
            "{\"numero\":\"123456789012345678901\",\"capacidade\":4}")) {
      MvcResult resultado =
          executar(post("/api/v1/mesas").contentType(MediaType.APPLICATION_JSON).content(corpo))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
              .andReturn();
      assertThat(json(resultado).get("code").asString()).isEqualTo("VALIDACAO_INVALIDA");
    }
    assertThat(admin.restauranteId()).isNotNull();
  }

  @Test
  void garcomECopaSomenteConsultamMesas() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    UUID mesaId = criarMesa(admin, "12", 4);
    for (Papel papel : List.of(Papel.GARCOM, Papel.COPA)) {
      criarUsuario(papel);
      executar(get("/api/v1/mesas")).andExpect(status().isOk());
      executar(
              post("/api/v1/mesas")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"numero\":\"13\",\"capacidade\":4}"))
          .andExpect(status().isForbidden());
      executar(
              patch("/api/v1/mesas/" + mesaId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"numero\":\"14\"}"))
          .andExpect(status().isForbidden());
    }
    mockMvc.perform(get("/api/v1/mesas")).andExpect(status().isUnauthorized());
  }

  @Test
  void statusDaMesaRefleteComandaAbertaEFechada() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    UUID mesaId = criarMesa(admin, "12", 4);
    inserirComanda(admin, mesaId, "ABERTA");

    assertThat(json(executar(get("/api/v1/mesas/" + mesaId)).andReturn()).get("status").asString())
        .isEqualTo("OCUPADA");
    assertThat(
            json(executar(get("/api/v1/mesas?status=OCUPADA")).andReturn())
                .get("items")
                .get(0)
                .get("status")
                .asString())
        .isEqualTo("OCUPADA");

    fecharComanda(admin, mesaId);
    assertThat(json(executar(get("/api/v1/mesas/" + mesaId)).andReturn()).get("status").asString())
        .isEqualTo("LIVRE");
  }

  @Test
  void listagemFiltraOrdenaEPaginaMesasOcupadas() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    for (int numero = 1; numero <= 21; numero++) {
      UUID mesaId = criarMesa(admin, String.valueOf(numero), 4);
      if (numero % 2 == 0) {
        inserirComanda(admin, mesaId, "ABERTA");
      }
    }

    JsonNode corpo =
        json(
            executar(get("/api/v1/mesas?status=OCUPADA&ativa=true&page=1&size=2"))
                .andExpect(status().isOk())
                .andReturn());
    assertThat(corpo.get("totalElements").asLong()).isEqualTo(10);
    assertThat(corpo.get("items").size()).isEqualTo(2);
    assertThat(corpo.get("items").get(0).get("numero").asString()).isEqualTo("14");
    assertThat(corpo.get("items").get(1).get("numero").asString()).isEqualTo("16");
  }

  @Test
  void desativacaoDeMesaOcupadaEPermitidaDepoisDoFechamento() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    UUID mesaId = criarMesa(admin, "12", 4);
    inserirComanda(admin, mesaId, "ABERTA");

    MvcResult erro =
        executar(
                patch("/api/v1/mesas/" + mesaId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ativa\":false}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andReturn();
    assertThat(json(erro).get("code").asString()).isEqualTo("MESA_OCUPADA");
    assertThat(json(executar(get("/api/v1/mesas/" + mesaId)).andReturn()).get("ativa").asBoolean())
        .isTrue();

    fecharComanda(admin, mesaId);
    executar(
            patch("/api/v1/mesas/" + mesaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativa\":false}"))
        .andExpect(status().isOk());
  }

  @Test
  void renomearMesaOcupadaPreservaStatusNaResposta() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    UUID mesaId = criarMesa(admin, "12", 4);
    inserirComanda(admin, mesaId, "ABERTA");

    JsonNode corpo =
        json(
            executar(
                    patch("/api/v1/mesas/" + mesaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"13\"}"))
                .andExpect(status().isOk())
                .andReturn());
    assertThat(corpo.get("status").asString()).isEqualTo("OCUPADA");
  }

  @Test
  void patchRejeitaPayloadsNulosVaziosDesconhecidosEInvalidos() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    UUID mesaId = criarMesa(admin, "12", 4);
    for (String corpo :
        List.of(
            "{}",
            "{\"numero\":null}",
            "{\"capacidade\":null}",
            "{\"ativa\":null}",
            "{\"status\":\"OCUPADA\"}",
            "{\"numero\":\"123456789012345678901\"}",
            "{\"capacidade\":0}",
            "{\"capacidade\":101}")) {
      MvcResult resultado =
          executar(
                  patch("/api/v1/mesas/" + mesaId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(corpo))
              .andExpect(status().isBadRequest())
              .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
              .andReturn();
      assertThat(json(resultado).get("code").asString()).isEqualTo("VALIDACAO_INVALIDA");
    }
    assertThat(admin.restauranteId()).isNotNull();
  }

  @Test
  void sizeMaiorQueCemEPaginaMuitoAltaRetornamErrosOuPaginaVazia() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    criarMesa(admin, "12", 4);
    executar(get("/api/v1/mesas?size=101"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    JsonNode pagina =
        json(executar(get("/api/v1/mesas?page=2147483647")).andExpect(status().isOk()).andReturn());
    assertThat(pagina.get("items")).isEmpty();
    assertThat(pagina.get("totalElements").asLong()).isEqualTo(1);
  }

  @Test
  void mesaDeOutroRestauranteNaoEVisivel() throws Exception {
    var primeiro = criarUsuario(Papel.ADMIN);
    UUID mesaId = criarMesa(primeiro, "12", 4);
    criarUsuario(Papel.ADMIN);

    executar(get("/api/v1/mesas/" + mesaId)).andExpect(status().isNotFound());
    executar(
            patch("/api/v1/mesas/" + mesaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":\"13\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void duasCriacoesConcorrentesRetornamUmaCriacaoEUmConflito() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    var inicio = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    var resultados = new AtomicInteger();
    var conflitos = new AtomicInteger();
    try {
      var tarefas =
          List.of(
              executor.submit(
                  () -> {
                    try {
                      criarConcorrente(admin, inicio, resultados, conflitos);
                    } catch (Exception exception) {
                      throw new RuntimeException(exception);
                    }
                  }),
              executor.submit(
                  () -> {
                    try {
                      criarConcorrente(admin, inicio, resultados, conflitos);
                    } catch (Exception exception) {
                      throw new RuntimeException(exception);
                    }
                  }));
      inicio.countDown();
      tarefas.forEach(
          tarefa -> {
            try {
              tarefa.get();
            } catch (Exception exception) {
              throw new RuntimeException(exception);
            }
          });
    } finally {
      executor.shutdownNow();
    }
    assertThat(resultados.get()).isEqualTo(1);
    assertThat(conflitos.get()).isEqualTo(1);
  }

  @Test
  void deleteDeMesaNaoEOperacaoPermitida() throws Exception {
    var admin = criarUsuario(Papel.ADMIN);
    UUID mesaId = criarMesa(admin, "12", 4);
    executar(delete("/api/v1/mesas/" + mesaId)).andExpect(status().is4xxClientError());
  }

  private void criarConcorrente(
      DadosDeAutenticacaoTeste admin,
      CountDownLatch inicio,
      AtomicInteger resultados,
      AtomicInteger conflitos)
      throws Exception {
    inicio.await();
    int status =
        executar(
                post("/api/v1/mesas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"numero\":\"concorrente\",\"capacidade\":4}"))
            .andReturn()
            .getResponse()
            .getStatus();
    if (status == 201) {
      resultados.incrementAndGet();
    } else if (status == 409) {
      conflitos.incrementAndGet();
    } else {
      throw new AssertionError("Status concorrente inesperado: " + status);
    }
  }

  private UUID criarMesa(DadosDeAutenticacaoTeste admin, String numero, int capacidade)
      throws Exception {
    return UUID.fromString(
        json(executar(
                    post("/api/v1/mesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"numero\":\"" + numero + "\",\"capacidade\":" + capacidade + "}"))
                .andReturn())
            .get("id")
            .asString());
  }

  private DadosDeAutenticacaoTeste criarUsuario(Papel papel) throws SQLException {
    tenantAtual =
        DadosDeAutenticacaoTeste.criar(usuarios, hashDeSenha, execucaoNoTenant, papel, true, true);
    papelAtual = papel;
    return tenantAtual;
  }

  private ResultActions executar(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
      throws Exception {
    return mockMvc.perform(
        request.with(
            jwt()
                .jwt(token -> token.claim("tenant_id", tenantAtual.restauranteId().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + papelAtual))));
  }

  private void inserirComanda(DadosDeAutenticacaoTeste admin, UUID mesaId, String status)
      throws SQLException {
    try (Connection connection = conexaoDona();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into comanda
                    (id, restaurante_id, mesa_id, status, aberta_por, fechada_em, fechada_por, total_final)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, admin.restauranteId());
      statement.setObject(3, mesaId);
      statement.setString(4, status);
      statement.setObject(5, admin.usuarioId());
      if ("FECHADA".equals(status)) {
        statement.setObject(6, Instant.now());
        statement.setObject(7, admin.usuarioId());
        statement.setBigDecimal(8, java.math.BigDecimal.ZERO);
      } else {
        statement.setObject(6, null);
        statement.setObject(7, null);
        statement.setObject(8, null);
      }
      statement.executeUpdate();
    }
  }

  private void fecharComanda(DadosDeAutenticacaoTeste admin, UUID mesaId) throws SQLException {
    try (Connection connection = conexaoDona();
        PreparedStatement statement =
            connection.prepareStatement(
                "update comanda set status = 'FECHADA', fechada_em = now(), fechada_por = ?, total_final = 0 where restaurante_id = ? and mesa_id = ? and status = 'ABERTA'")) {
      statement.setObject(1, admin.usuarioId());
      statement.setObject(2, admin.restauranteId());
      statement.setObject(3, mesaId);
      statement.executeUpdate();
    }
  }

  private Connection conexaoDona() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private JsonNode json(MvcResult resultado) throws Exception {
    return objectMapper.readTree(resultado.getResponse().getContentAsString());
  }
}
