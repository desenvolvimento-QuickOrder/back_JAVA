package com.quickorder.restaurant.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionarioDuplicado;
import com.quickorder.restaurant.core.equipe.domain.HashDeSenha;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.tenant.ExecucaoNoTenant;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class UsuariosCrudIT extends IntegracaoBaseIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private HashDeSenha hashDeSenha;
  @Autowired private RepositorioDeUsuarios usuarios;
  @Autowired private ExecucaoNoTenant execucaoNoTenant;

  @Test
  void criaAdminERejeitaCredenciaisInvalidasOuLoginDuplicado() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String corpo =
        "{\"nome\":\"Outro admin\",\"papel\":\"ADMIN\",\"login\":\"outro.admin\",\"senha\":\"senha-forte-123\"}";

    MvcResult criado =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
            .andReturn();
    assertThat(criado.getResponse().getStatus()).isEqualTo(201);
    assertThat(criado.getResponse().getHeader("Location")).contains("/api/v1/usuarios/");
    assertThat(criado.getResponse().getContentAsString()).doesNotContain("senha-forte-123");

    MvcResult duplicado =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
            .andReturn();
    assertThat(duplicado.getResponse().getStatus()).isEqualTo(409);
    assertThat(codigo(duplicado)).isEqualTo("LOGIN_JA_EXISTE");

    UUID usuarioCriado =
        UUID.fromString(
            objectMapper.readTree(criado.getResponse().getContentAsString()).get("id").asString());
    assertThat(desativar(admin, usuarioCriado).getResponse().getStatus()).isEqualTo(200);
    MvcResult loginDeUsuarioInativo =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
            .andReturn();
    assertThat(loginDeUsuarioInativo.getResponse().getStatus()).isEqualTo(409);

    DadosDeAutenticacaoTeste outroTenant = criarAdmin();
    MvcResult mesmoLoginEmOutroTenant =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(outroTenant))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
            .andReturn();
    assertThat(mesmoLoginEmOutroTenant.getResponse().getStatus()).isEqualTo(201);

    MvcResult papelInvalido =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\"Garçom\",\"papel\":\"GARCOM\",\"login\":\"garcom\",\"senha\":\"senha-forte-123\"}"))
            .andReturn();
    assertThat(papelInvalido.getResponse().getStatus()).isEqualTo(400);
    assertThat(
            objectMapper.readTree(papelInvalido.getResponse().getContentAsString()).get("errors"))
        .isNotNull();
  }

  @Test
  void criaFuncionarioComCodigoSemExporPinEValidaCredenciaisPorPapel() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    MvcResult criado =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Garçom\",\"papel\":\"GARCOM\",\"pin\":\"2580\"}"))
            .andReturn();
    assertThat(criado.getResponse().getStatus()).isEqualTo(201);
    var resposta = objectMapper.readTree(criado.getResponse().getContentAsString());
    String codigoGerado = resposta.get("codigo").asString();
    assertThat(codigoGerado).matches("\\d{4}").isNotEqualTo("2580");
    assertThat(resposta.has("pin")).isFalse();
    assertThat(criado.getResponse().getContentAsString()).doesNotContain("2580");
    UUID funcionarioId = UUID.fromString(resposta.get("id").asString());

    MvcResult obtido =
        mockMvc.perform(get("/api/v1/usuarios/{id}", funcionarioId).with(token(admin))).andReturn();
    assertThat(
            objectMapper
                .readTree(obtido.getResponse().getContentAsString())
                .get("codigo")
                .asString())
        .isEqualTo(codigoGerado);

    MvcResult listado =
        mockMvc
            .perform(get("/api/v1/usuarios").with(token(admin)).queryParam("papel", "GARCOM"))
            .andReturn();
    assertThat(
            objectMapper
                .readTree(listado.getResponse().getContentAsString())
                .get("items")
                .get(0)
                .get("codigo")
                .asString())
        .isEqualTo(codigoGerado);

    MvcResult semPin =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Garçom\",\"papel\":\"GARCOM\"}"))
            .andReturn();
    assertThat(semPin.getResponse().getStatus()).isEqualTo(400);
    assertThat(camposDeErro(semPin)).contains("pin");

    for (String pinProibido : List.of("1111", "1234", "4321")) {
      MvcResult pinInvalido =
          mockMvc
              .perform(
                  post("/api/v1/usuarios")
                      .with(token(admin))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"nome\":\"Garçom\",\"papel\":\"GARCOM\",\"pin\":\""
                              + pinProibido
                              + "\"}"))
              .andReturn();
      assertThat(pinInvalido.getResponse().getStatus()).isEqualTo(400);
      assertThat(camposDeErro(pinInvalido)).contains("pin");
    }

    MvcResult credenciaisDeAdminEmFuncionario =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\"Garçom\",\"papel\":\"GARCOM\",\"login\":\"garcom\","
                            + "\"senha\":\"senha-forte-123\",\"pin\":\"2581\"}"))
            .andReturn();
    assertThat(credenciaisDeAdminEmFuncionario.getResponse().getStatus()).isEqualTo(400);
    assertThat(camposDeErro(credenciaisDeAdminEmFuncionario)).contains("login", "senha");

    MvcResult loginNuloEmFuncionario =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\"Garçom\",\"papel\":\"GARCOM\",\"login\":null,\"pin\":\"2581\"}"))
            .andReturn();
    assertThat(loginNuloEmFuncionario.getResponse().getStatus()).isEqualTo(400);
    assertThat(camposDeErro(loginNuloEmFuncionario)).contains("login");

    MvcResult pinEmAdmin =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\"Admin\",\"papel\":\"ADMIN\",\"login\":\"novo.admin\","
                            + "\"senha\":\"senha-forte-123\",\"pin\":\"2581\"}"))
            .andReturn();
    assertThat(pinEmAdmin.getResponse().getStatus()).isEqualTo(400);
    assertThat(camposDeErro(pinEmAdmin)).contains("pin");

    MvcResult adminSemLoginOuSenha =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Admin\",\"papel\":\"ADMIN\"}"))
            .andReturn();
    assertThat(adminSemLoginOuSenha.getResponse().getStatus()).isEqualTo(400);
    assertThat(camposDeErro(adminSemLoginOuSenha)).contains("login", "senha");

    MvcResult pinNuloEmAdmin =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nome\":\"Admin\",\"papel\":\"ADMIN\",\"login\":\"nulo.admin\","
                            + "\"senha\":\"senha-forte-123\",\"pin\":null}"))
            .andReturn();
    assertThat(pinNuloEmAdmin.getResponse().getStatus()).isEqualTo(400);
    assertThat(camposDeErro(pinNuloEmAdmin)).contains("pin");
  }

  @Test
  void codigoDeFuncionarioInativoNaoEReutilizado() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    MvcResult primeiro =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Primeiro\",\"papel\":\"GARCOM\",\"pin\":\"2580\"}"))
            .andReturn();
    var respostaPrimeiro = objectMapper.readTree(primeiro.getResponse().getContentAsString());
    UUID idPrimeiro = UUID.fromString(respostaPrimeiro.get("id").asString());
    String codigoPrimeiro = respostaPrimeiro.get("codigo").asString();
    assertThat(desativar(admin, idPrimeiro).getResponse().getStatus()).isEqualTo(200);

    MvcResult segundo =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Segundo\",\"papel\":\"COPA\",\"pin\":\"3579\"}"))
            .andReturn();
    assertThat(segundo.getResponse().getStatus()).isEqualTo(201);
    assertThat(
            objectMapper
                .readTree(segundo.getResponse().getContentAsString())
                .get("codigo")
                .asString())
        .isNotEqualTo(codigoPrimeiro);
  }

  @Test
  void adaptadorIdentificaColisaoDaConstraintDeCodigo() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    Usuario primeiro =
        Usuario.novoFuncionario(
            UUID.randomUUID(),
            admin.restauranteId(),
            "Primeiro",
            Papel.GARCOM,
            "9876",
            hashDeSenha.gerar("2580"));
    Usuario duplicado =
        Usuario.novoFuncionario(
            UUID.randomUUID(),
            admin.restauranteId(),
            "Segundo",
            Papel.COPA,
            "9876",
            hashDeSenha.gerar("3579"));
    execucaoNoTenant.executar(admin.restauranteId(), () -> usuarios.salvar(primeiro));

    Throwable erro =
        catchThrowable(
            () ->
                execucaoNoTenant.executar(admin.restauranteId(), () -> usuarios.salvar(duplicado)));

    assertThat(erro).isInstanceOf(CodigoFuncionarioDuplicado.class);
  }

  @Test
  void patchDePinRejeitaAdminECodigoDoFuncionario() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    MvcResult criado =
        mockMvc
            .perform(
                post("/api/v1/usuarios")
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Garçom\",\"papel\":\"GARCOM\",\"pin\":\"2580\"}"))
            .andReturn();
    UUID funcionarioId =
        UUID.fromString(
            objectMapper.readTree(criado.getResponse().getContentAsString()).get("id").asString());
    String codigoFuncionario =
        objectMapper.readTree(criado.getResponse().getContentAsString()).get("codigo").asString();

    MvcResult pinIgualAoCodigo =
        mockMvc
            .perform(
                patch("/api/v1/usuarios/{id}", funcionarioId)
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"pin\":\"" + codigoFuncionario + "\"}"))
            .andReturn();
    assertThat(pinIgualAoCodigo.getResponse().getStatus()).isEqualTo(400);
    assertThat(camposDeErro(pinIgualAoCodigo)).contains("pin");

    MvcResult pinEmAdmin =
        mockMvc
            .perform(
                patch("/api/v1/usuarios/{id}", admin.usuarioId())
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"pin\":\"2581\"}"))
            .andReturn();
    assertThat(pinEmAdmin.getResponse().getStatus()).isEqualTo(400);
    assertThat(camposDeErro(pinEmAdmin)).contains("pin");
  }

  @Test
  void listaComFiltrosPaginacaoOrdenacaoETamanhoMaximo() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    criarOutroAdmin(admin.restauranteId(), "z-admin");
    criarOutroAdmin(admin.restauranteId(), "a-admin");

    MvcResult resultado =
        mockMvc
            .perform(
                get("/api/v1/usuarios")
                    .with(token(admin))
                    .queryParam("papel", "ADMIN")
                    .queryParam("ativo", "true")
                    .queryParam("page", "0")
                    .queryParam("size", "2"))
            .andReturn();
    var corpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
    assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    assertThat(corpo.get("total").asLong()).isEqualTo(3);
    assertThat(corpo.get("items").size()).isEqualTo(2);
    assertThat(corpo.get("items").get(0).get("nome").asString())
        .isLessThanOrEqualTo(corpo.get("items").get(1).get("nome").asString());

    MvcResult tamanhoExcedido =
        mockMvc
            .perform(get("/api/v1/usuarios").with(token(admin)).queryParam("size", "101"))
            .andReturn();
    assertThat(tamanhoExcedido.getResponse().getStatus()).isEqualTo(400);
    var erroDeTamanho = objectMapper.readTree(tamanhoExcedido.getResponse().getContentAsString());
    assertThat(erroDeTamanho.get("code").asString()).isEqualTo("VALIDACAO_INVALIDA");
    assertThat(erroDeTamanho.get("errors").get(0).get("campo").asString()).isEqualTo("size");
  }

  @Test
  void patchEParcialEValorNuloOuCorpoVazioSaoRejeitados() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();

    MvcResult atualizado =
        mockMvc
            .perform(
                patch("/api/v1/usuarios/{id}", admin.usuarioId())
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Nome atualizado\"}"))
            .andReturn();
    assertThat(atualizado.getResponse().getStatus()).isEqualTo(200);
    assertThat(
            objectMapper
                .readTree(atualizado.getResponse().getContentAsString())
                .get("nome")
                .asString())
        .isEqualTo("Nome atualizado");

    MvcResult nuloExplicito =
        mockMvc
            .perform(
                patch("/api/v1/usuarios/{id}", admin.usuarioId())
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":null}"))
            .andReturn();
    assertThat(nuloExplicito.getResponse().getStatus()).isEqualTo(400);
    assertThat(
            objectMapper.readTree(nuloExplicito.getResponse().getContentAsString()).get("errors"))
        .isNotNull();

    MvcResult vazio =
        mockMvc
            .perform(
                patch("/api/v1/usuarios/{id}", admin.usuarioId())
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andReturn();
    assertThat(vazio.getResponse().getStatus()).isEqualTo(400);
  }

  @Test
  void idDeOutroTenantRetorna404EPapelAdminNaoPodeSerTrocado() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    DadosDeAutenticacaoTeste outroTenant = criarAdmin();

    MvcResult ausente =
        mockMvc
            .perform(get("/api/v1/usuarios/{id}", outroTenant.usuarioId()).with(token(admin)))
            .andReturn();
    assertThat(ausente.getResponse().getStatus()).isEqualTo(404);

    MvcResult trocaDePapel =
        mockMvc
            .perform(
                patch("/api/v1/usuarios/{id}", admin.usuarioId())
                    .with(token(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"papel\":\"GARCOM\"}"))
            .andReturn();
    assertThat(trocaDePapel.getResponse().getStatus()).isEqualTo(422);
    assertThat(codigo(trocaDePapel)).isEqualTo("TROCA_DE_PAPEL_NAO_PERMITIDA");
  }

  @Test
  void propriaSenhaExigeSenhaAtualERevogaRefreshTokens() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    String refresh = refresh(admin);

    MvcResult semSenhaAtual = atualizarSenha(admin, "{\"senha\":\"senha-nova-123\"}");
    assertThat(semSenhaAtual.getResponse().getStatus()).isEqualTo(422);
    assertThat(codigo(semSenhaAtual)).isEqualTo("SENHA_ATUAL_INVALIDA");

    MvcResult senhaAtualInvalida =
        atualizarSenha(admin, "{\"senha\":\"senha-nova-123\",\"senhaAtual\":\"incorreta-123\"}");
    assertThat(senhaAtualInvalida.getResponse().getStatus()).isEqualTo(422);

    MvcResult sucesso =
        atualizarSenha(
            admin, "{\"senha\":\"senha-nova-123\",\"senhaAtual\":\"senha-forte-de-teste-123\"}");
    assertThat(sucesso.getResponse().getStatus()).isEqualTo(200);

    MvcResult refreshRevogado =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + refresh + "\"}"))
            .andReturn();
    assertThat(refreshRevogado.getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void desativacaoEAtivacaoSaoIdempotentesMasPreservamUltimoAdmin() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    assertThat(desativar(admin, admin.usuarioId()).getResponse().getStatus()).isEqualTo(422);

    UUID outroAdmin = criarOutroAdmin(admin.restauranteId(), "segundo-admin");
    DadosDeAutenticacaoTeste segundo =
        new DadosDeAutenticacaoTeste(
            admin.restauranteId(), outroAdmin, admin.slug(), "segundo-admin", "senha-forte-123");
    String refresh = refresh(segundo);
    assertThat(desativar(admin, outroAdmin).getResponse().getStatus()).isEqualTo(200);
    assertThat(desativar(admin, outroAdmin).getResponse().getStatus()).isEqualTo(200);
    assertThat(ativar(admin, outroAdmin).getResponse().getStatus()).isEqualTo(200);
    assertThat(ativar(admin, outroAdmin).getResponse().getStatus()).isEqualTo(200);

    MvcResult refreshRevogado =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + refresh + "\"}"))
            .andReturn();
    assertThat(refreshRevogado.getResponse().getStatus()).isEqualTo(401);
  }

  @Test
  void criacoesConcorrentesComMesmoLoginRetornam201E409() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    CountDownLatch inicio = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> primeiro = executor.submit(() -> criarComLogin(admin, "concorrente", inicio));
      Future<Integer> segundo = executor.submit(() -> criarComLogin(admin, "concorrente", inicio));
      inicio.countDown();
      List<Integer> status = List.of(primeiro.get(), segundo.get());
      assertThat(status).containsExactlyInAnyOrder(201, 409);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void doisAdminsDesativandoSeAoMesmoTempoDeixamUmAtivo() throws Exception {
    DadosDeAutenticacaoTeste primeiro = criarAdmin();
    UUID segundoId = criarOutroAdmin(primeiro.restauranteId(), "segundo-admin");
    CountDownLatch inicio = new CountDownLatch(1);
    var executor = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> respostaPrimeiro =
          executor.submit(
              () ->
                  desativarConcorrentemente(
                      primeiro, primeiro.usuarioId(), primeiro.usuarioId(), inicio));
      Future<Integer> respostaSegundo =
          executor.submit(() -> desativarConcorrentemente(primeiro, segundoId, segundoId, inicio));
      inicio.countDown();
      assertThat(List.of(respostaPrimeiro.get(), respostaSegundo.get()))
          .containsExactlyInAnyOrder(200, 422);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void papelGarcomNaoAcessaRotasAdministrativasDeUsuarios() throws Exception {
    DadosDeAutenticacaoTeste admin = criarAdmin();
    MvcResult resposta =
        mockMvc
            .perform(
                get("/api/v1/usuarios")
                    .with(
                        jwt()
                            .jwt(
                                jwt ->
                                    jwt.subject(UUID.randomUUID().toString())
                                        .claim("tenant_id", admin.restauranteId().toString())
                                        .claim("roles", List.of("GARCOM")))
                            .authorities(new SimpleGrantedAuthority("ROLE_GARCOM"))))
            .andReturn();
    assertThat(resposta.getResponse().getStatus()).isEqualTo(403);
  }

  private DadosDeAutenticacaoTeste criarAdmin() throws SQLException {
    return DadosDeAutenticacaoTeste.criar(
        usuarios, hashDeSenha, execucaoNoTenant, Papel.ADMIN, true, true);
  }

  private UUID criarOutroAdmin(UUID restauranteId, String login) {
    UUID id = UUID.randomUUID();
    execucaoNoTenant.executar(
        restauranteId,
        () ->
            usuarios.salvar(
                Usuario.novoAdmin(
                    id, restauranteId, login, login, hashDeSenha.gerar("senha-forte-123"))));
    return id;
  }

  private RequestPostProcessor token(DadosDeAutenticacaoTeste admin) {
    return token(admin.restauranteId(), admin.usuarioId());
  }

  private RequestPostProcessor token(UUID restauranteId, UUID usuarioId) {
    return jwt()
        .jwt(
            jwt ->
                jwt.subject(usuarioId.toString())
                    .claim("tenant_id", restauranteId.toString())
                    .claim("roles", List.of("ADMIN")))
        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private MvcResult atualizarSenha(DadosDeAutenticacaoTeste admin, String corpo) throws Exception {
    return mockMvc
        .perform(
            patch("/api/v1/usuarios/{id}", admin.usuarioId())
                .with(token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
        .andReturn();
  }

  private String refresh(DadosDeAutenticacaoTeste admin) throws Exception {
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"restaurante\":\""
                            + admin.slug()
                            + "\",\"login\":\""
                            + admin.login()
                            + "\",\"senha\":\""
                            + admin.senha()
                            + "\"}"))
            .andReturn();
    return objectMapper
        .readTree(login.getResponse().getContentAsString())
        .get("refreshToken")
        .asString();
  }

  private MvcResult desativar(DadosDeAutenticacaoTeste admin, UUID id) throws Exception {
    return mockMvc
        .perform(post("/api/v1/usuarios/{id}/desativar", id).with(token(admin)))
        .andReturn();
  }

  private MvcResult ativar(DadosDeAutenticacaoTeste admin, UUID id) throws Exception {
    return mockMvc.perform(post("/api/v1/usuarios/{id}/ativar", id).with(token(admin))).andReturn();
  }

  private int criarComLogin(DadosDeAutenticacaoTeste admin, String login, CountDownLatch inicio)
      throws Exception {
    inicio.await();
    return mockMvc
        .perform(
            post("/api/v1/usuarios")
                .with(token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nome\":\"Concorrente\",\"papel\":\"ADMIN\",\"login\":\""
                        + login
                        + "\",\"senha\":\"senha-forte-123\"}"))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private int desativarConcorrentemente(
      DadosDeAutenticacaoTeste admin, UUID ator, UUID alvo, CountDownLatch inicio)
      throws Exception {
    inicio.await();
    return mockMvc
        .perform(
            post("/api/v1/usuarios/{id}/desativar", alvo).with(token(admin.restauranteId(), ator)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private String codigo(MvcResult resultado) throws Exception {
    return objectMapper
        .readTree(resultado.getResponse().getContentAsString())
        .get("code")
        .asString();
  }

  private List<String> camposDeErro(MvcResult resultado) throws Exception {
    return objectMapper
        .readTree(resultado.getResponse().getContentAsString())
        .get("errors")
        .findValues("campo")
        .stream()
        .map(erro -> erro.asString())
        .toList();
  }
}
